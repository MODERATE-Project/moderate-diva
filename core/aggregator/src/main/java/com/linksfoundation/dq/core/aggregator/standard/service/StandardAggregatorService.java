package com.linksfoundation.dq.core.aggregator.standard.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.linksfoundation.dq.api.aggregator.service.AggregatorManager;
import com.linksfoundation.dq.api.aggregator.service.AggregatorService;
import com.linksfoundation.dq.core.aggregator.standard.exceptions.RuleBadFormatted;
import com.linksfoundation.dq.core.aggregator.standard.exceptions.YamlBadFormatted;
import com.linksfoundation.dq.core.aggregator.standard.schema.ConfigYaml;
import com.linksfoundation.dq.core.aggregator.standard.schema.Dataset;
import com.linksfoundation.dq.core.aggregator.standard.schema.Specs;
import com.linksfoundation.dq.api.model.BoolArray;
import com.linksfoundation.dq.api.model.FloatArray;
import com.linksfoundation.dq.api.model.Sample;
import com.linksfoundation.dq.api.model.StringArray;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.util.function.Tuples;

import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.HTreeMap;
import org.mapdb.Serializer;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Slf4j
public class StandardAggregatorService extends AggregatorService implements CommandLineRunner{

    @Value(value = "${CONFIG_FILE}")
    private String configFile;
    private Map<String, Dataset> datasetsConfig;
    //private DB db;
    //private HTreeMap<String, HTreeMap<String, Sample>> datasets;
    private ConcurrentMap<String, ConcurrentMap<String, Sample>> db;

    protected StandardAggregatorService(AggregatorManager manager) {
        super(manager);
    }

    public Flux<Sample> aggregate(Sample sample) {
        String name = sample.getDataset();
        if (datasetsConfig.containsKey(name)) {
            this.addSampleToMap(datasetsConfig.get(name), sample);
        }

        List<Sample> samples = new LinkedList<>();
        String key = getKey(datasetsConfig.get(name).getKey(), sample);

        for(Entry<String, ConcurrentMap<String, Sample>> dataset : this.db.entrySet()) {
            if (datasetsConfig.keySet().contains(dataset.getKey()) &&
                dataset.getValue().containsKey(key)) {
                samples.add(dataset.getValue().get(key));
            }
        }

        if (samples.size() > 1) {
            return aggregateSamples(samples);
        } else {
            return Flux.empty();
        }
    }

    protected ConfigYaml parseYamlFile(String path) {
        ObjectMapper om = new ObjectMapper(new YAMLFactory());

        try {
            File file = new File(path);
            return om.readValue(file, ConfigYaml.class);

        } catch (IOException e) {
            throw new YamlBadFormatted();
        }
    }

    protected Object parseYamlObject(Object specs, Class<? extends Specs> schema) {
        ObjectMapper om = new ObjectMapper();
        om.setSerializationInclusion(JsonInclude.Include.ALWAYS);

        try {
            byte[] json = om.writeValueAsBytes(specs);
            return om.readValue(json, schema);
        } catch (IOException e) {
            throw new RuleBadFormatted();
        }
    }

    private void addSampleToMap(Dataset dataset, Sample sample) {
        String key = getKey(dataset.getKey(), sample);

        if (this.db.containsKey(dataset.getName())) {
            this.db.get(dataset.getName()).put(key, sample);
        } else {
            /*
            HTreeMap<String, Sample> innerMap = db
                .hashMap(dataset, Serializer.STRING, new ProtobufSerializer())
                .createOrOpen();
            */

            ConcurrentMap<String, Sample> innerMap = new ConcurrentHashMap<>();
            innerMap.put(key, sample);
            this.db.put(dataset.getName(), innerMap);
        }
    }

    private String getKey(String key, Sample sample) {
        if (sample.getBoolDataMap().containsKey(key)) {
            return sample.getBoolDataMap().get(key).toString();
        }
        else if(sample.getFloatDataMap().containsKey(key)) {
            return sample.getFloatDataMap().get(key).toString();
        }
        else if(sample.getStringDataMap().containsKey(key)) {
            return sample.getStringDataMap().get(key).toString();
        }

        return "";
    }

    private Flux<Sample> aggregateSamples(List<Sample> samples) {
        Map<String, FloatArray> totalFloatData = new HashMap<>();
        Map<String, BoolArray> totalBoolData = new HashMap<>();
        Map<String, StringArray> totalStringData = new HashMap<>();
        Set<String> datasets = new HashSet<>();

        final String KEY_FORMAT = "%s-%s";

        for (int i = 0; i < samples.size(); i++) {

            Sample currentSample= samples.get(i);
            String currentKey = this.datasetsConfig.get(currentSample.getDataset()).getKey();
            String newKey = KEY_FORMAT.formatted(currentSample.getDataset(), currentKey);
            
            Map<String, FloatArray> sampleFloatData = currentSample.getFloatDataMap().entrySet().stream()
                    .collect(Collectors.toMap(
                            e ->  KEY_FORMAT.formatted(currentSample.getDataset(), e.getKey()),
                            Map.Entry::getValue)
                    );

            Map<String, BoolArray> sampleBoolData = currentSample.getBoolDataMap().entrySet().stream()
                    .collect(Collectors.toMap(
                            e -> KEY_FORMAT.formatted(currentSample.getDataset(), e.getKey()),
                            Map.Entry::getValue)
                    );

            Map<String, StringArray> sampleStringData = currentSample.getStringDataMap().entrySet().stream()
                    .collect(Collectors.toMap(
                            e -> KEY_FORMAT.formatted(currentSample.getDataset(), e.getKey()),
                            Map.Entry::getValue)
                    );

            if (i > 0) {
                if (sampleFloatData.containsKey(newKey)) {
                    sampleFloatData.remove(newKey);
                } else if (sampleBoolData.containsKey(newKey)) {
                    sampleBoolData.remove(newKey);
                } else {
                    sampleStringData.remove(newKey);
                }

            }

            datasets.add(currentSample.getDataset());
            totalFloatData.putAll(sampleFloatData);
            totalBoolData.putAll(sampleBoolData);
            totalStringData.putAll(sampleStringData);
        }
        
        Map<String, String> totalMetadata = samples
            .stream()
            .map(
                s -> s.getMetadataMap()
                .entrySet()
                .stream()
                .collect(Collectors.toMap(
                    e -> String.format("%s-%s", s.getDataset(), e.getKey()), 
                    Entry::getValue))
            )
            .flatMap(m -> m.entrySet().stream())
            .collect(Collectors.toMap(Entry::getKey, Entry::getValue));

        long totalTs = samples.stream().mapToLong(s -> s.getTs()).max().orElse(0);
        return Flux.just(
            Sample.newBuilder()
                .setState(Sample.States.AGGREGATED)
                .setTs(totalTs)
                .setDataset(datasets.stream().collect(Collectors.joining(",")))
                .putAllFloatData(totalFloatData)
                .putAllBoolData(totalBoolData)
                .putAllStringData(totalStringData)
                .putAllMetadata(totalMetadata)
                .build()
        );
    }
    
    @Override
    public void run (String ...args) {
        ConfigYaml yaml = this.parseYamlFile(configFile);
        this.datasetsConfig = yaml.getDatasets().stream()
            .collect(Collectors.toMap(
                Dataset::getName, 
                d -> d));

        //this.db = DBMaker.memoryDB().make();
        //this.datasets = db.hashMap("datasets", Serializer.STRING, Serializer.JAVA).createOrOpen();
        this.db = new ConcurrentHashMap<>();

        log.info("Starting");
        this.receive().subscribe();

        long minInterval = datasetsConfig.values().stream()
            .mapToLong(d -> Duration.parse("PT" + d.getExpiration().toUpperCase()).toMillis())
            .min()
            .orElse(30000000);

        ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor();
        exec.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                for(Dataset dataset : datasetsConfig.values()) {
                    if (dataset.getExpiration().length() > 0 && db.containsKey(dataset.getName())) {
                        for (Entry<String, Sample> key : db.get(dataset.getName()).entrySet()) {
                            if (this.isExpired(dataset, key.getValue())) {
                                db.get(dataset.getName()).remove(key.getKey());
                            }
                        }
                    }
                }
            }

            private boolean isExpired(Dataset dataset, Sample sample) {
                long now = System.currentTimeMillis();
                long difference = now - sample.getTs();
    
                String interval = dataset.getExpiration();
                long duration = Duration.parse("PT" + interval.toUpperCase()).toMillis();
    
                return difference > duration;
            }

        }, 0, (long) 0.95*minInterval, TimeUnit.MILLISECONDS);
    }
}
