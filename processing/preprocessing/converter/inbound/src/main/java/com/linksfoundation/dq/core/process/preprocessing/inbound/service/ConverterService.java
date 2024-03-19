package com.linksfoundation.dq.core.process.preprocessing.inbound.service;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonSyntaxException;
import com.linksfoundation.dq.api.model.BoolArray;
import com.linksfoundation.dq.api.model.FloatArray;
import com.linksfoundation.dq.api.model.Sample;
import com.linksfoundation.dq.api.model.StringArray;
import com.linksfoundation.dq.api.processing.preprocessing.service.PreprocessingManager;
import com.linksfoundation.dq.api.processing.preprocessing.service.PreprocessingService;
import com.linksfoundation.dq.api.utils.exceptions.MQTTMessageNotDeserializable;
import com.linksfoundation.dq.core.process.preprocessing.inbound.model.DataCollectionModel;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ConverterService extends PreprocessingService {
    protected ConverterService(PreprocessingManager manager) {
        super(manager);
    }

    public Flux<Sample> preprocess(Sample sample) {
        try {
            List<Sample> results = new LinkedList<>();

            Gson gson = new Gson();
            String json = sample.getStringDataMap().get("JSON").getElement(0);
            DataCollectionModel model = gson.fromJson(json, DataCollectionModel.class);
            String dataset = model.getDataItemID();

            if (model.getMeasureUnit() != null) {
                if (model.getMeasureUnit().equals("List")) {
                    for (JsonElement element : model.getMetricValue().getAsJsonArray()) {
                        HashMap<String, Object> value = gson.fromJson(element.toString(), HashMap.class);
                        results.add(parseMetricValue(value, dataset, model.getMetricTypeID()));
                    }
                } else {
                    HashMap<String, Object> element = gson.fromJson(model.toString(), HashMap.class);
                    results.add(parseMetricValue(element, dataset, model.getMetricTypeID()));
                }
            } else {
                HashMap<String, Object> element = gson.fromJson(model.toString(), HashMap.class);
                results.add(parseMetricValue(element, dataset, model.getMetricTypeID()));
            }

            return Flux.fromIterable(
                    results.stream()
                            .map(s -> Sample.newBuilder(s)
                                    .putMetadata("sourceID", model.getSourceID())
                                    .putMetadata("sourceType", model.getSourceType())
                                    .putMetadata("metricTypeID", model.getMetricTypeID())
                                    .putMetadata("infoType", model.getInfoType())
                                    .putMetadata("dataType", model.getDataType())
                                    .putMetadata("dataItemID", model.getDataItemID())
                                    .build())
                            .collect(Collectors.toList()));

        } catch (JsonSyntaxException e) {
            throw new MQTTMessageNotDeserializable();
        }
    }

    private Sample parseMetricValue(Map<String, Object> metricValue, String dataset, String key) {

        Map<String, FloatArray.Builder> floatValues = new HashMap<>();
        Map<String, BoolArray.Builder> boolValues = new HashMap<>();
        Map<String, StringArray.Builder> stringValues = new HashMap<>();

        for (Map.Entry<String, Object> item : metricValue.entrySet()) {
            String itemKey = item.getKey();
            String itemValue = (String) item.getValue();

            if (itemValue != null) {
                boolean parsed = false;
                parsed = this.parseFloat(itemValue, itemKey, floatValues);

                if (!parsed) {
                    parsed = parseBoolean(itemValue, itemKey, boolValues);
                }

                if (!parsed) {
                    if (stringValues.containsKey(itemKey)) {
                        stringValues.get(itemKey).addElement(itemValue);
                    } else {
                        stringValues.put(itemKey, StringArray.newBuilder().addElement(itemValue));
                    }
                }
            }
        }

        return Sample.newBuilder()
                .setState(Sample.States.NEW)
                .clearFloatData()
                .clearBoolData()
                .clearStringData()
                .setDataset(dataset)
                .setKey(key)
                .putAllFloatData(floatValues.entrySet()
                        .stream().collect(Collectors.toMap(
                                Map.Entry::getKey,
                                e -> e.getValue().build()
                        )))
                .putAllBoolData(boolValues.entrySet()
                        .stream().collect(Collectors.toMap(
                                Map.Entry::getKey,
                                e -> e.getValue().build()
                        )))
                .putAllStringData(stringValues.entrySet()
                        .stream().collect(Collectors.toMap(
                                Map.Entry::getKey,
                                e -> e.getValue().build()
                        )))
                .build();
    }

    private boolean parseFloat(String field, String header, Map<String, FloatArray.Builder> map) {
        boolean success = false;
        try {

            float value;
            if (field.equals("")) {
                value = Float.NaN;
            } else {
                value = Float.parseFloat(field);
            }

            if (map.containsKey(header)) {
                map.get(header).addElement(value);
            }
            else {
                map.put(header, FloatArray.newBuilder().addElement(value));
            }

            success = true;
        }
        catch (NumberFormatException e) {
            success = false;
        }

        return success;
    }

    private boolean parseBoolean(String field, String header, Map<String, BoolArray.Builder> map) {

        boolean success = false;
        Boolean value = null;
        if (field.equals("true") || field.equals("True")) {
            value = Boolean.TRUE;
        }
        if (field.equals("false") || field.equals("False")) {
            value = Boolean.FALSE;
        }

        if (value != null) {
            if (map.containsKey(header)) {
                map.get(header).addElement(value);
            }
            else {
                map.put(header, BoolArray.newBuilder().addElement(value));
            }

            success = true;
        }

        return success;
    }
}
