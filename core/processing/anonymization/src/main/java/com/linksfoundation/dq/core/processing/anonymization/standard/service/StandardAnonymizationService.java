package com.linksfoundation.dq.core.processing.anonymization.standard.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import com.linksfoundation.dq.api.model.BoolArray;
import com.linksfoundation.dq.api.model.FloatArray;
import com.linksfoundation.dq.api.model.Sample;
import com.linksfoundation.dq.api.model.StringArray;
import com.linksfoundation.dq.api.model.Sample.States;
import com.linksfoundation.dq.api.processing.anonymization.service.AnonymizationManager;
import com.linksfoundation.dq.api.processing.anonymization.service.AnonymizationService;
import com.linksfoundation.dq.core.processing.anonymization.standard.exceptions.RuleBadFormatted;
import com.linksfoundation.dq.core.processing.anonymization.standard.exceptions.RuleNotRecognized;
import com.linksfoundation.dq.core.processing.anonymization.standard.exceptions.YamlBadFormatted;
import com.linksfoundation.dq.core.processing.anonymization.standard.schema.ConfigYaml;
import com.linksfoundation.dq.core.processing.anonymization.standard.schema.NormalizationSpecs;
import com.linksfoundation.dq.core.processing.anonymization.standard.schema.PseudonymizationSpecs;
import com.linksfoundation.dq.core.processing.anonymization.standard.schema.RotationSpecs;
import com.linksfoundation.dq.core.processing.anonymization.standard.schema.Rule;
import com.linksfoundation.dq.core.processing.anonymization.standard.schema.Specs;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedList;
import java.util.List;

/**
 * This class represents a StandardAnonymizationService that extends the AnonymizationService.
 * It provides methods to anonymize samples based on specified rules.
*/
@Service
@Slf4j
public class StandardAnonymizationService extends AnonymizationService {

    @Value(value = "${CONFIG_FILE}")
    private String configFile;
    private static final String ROTATION_LABEL = "rotation";
    private static final String NORMALIZATION_LABEL = "normalization";
    private static final String SUPPRESSION_LABEL = "suppression";
    private static final String PSEUDONYMIZATION_LABEL = "pseudonymization";

    protected StandardAnonymizationService(AnonymizationManager manager) {
        super(manager);
    }

    /**
     * Anonymizes the provided sample based on the configured rules.
     *
     * @param sample The sample to be anonymized.
     * @return A Flux emitting the anonymized sample.
    */
    @Override
    public Flux<Sample> anonymize(Sample sample) {
        ConfigYaml yaml = this.parseYamlFile(configFile);

        Sample newSample = Sample.newBuilder(sample).build();
        for (Rule rule : yaml.getRules()) {
            switch (rule.getName()) {
                case ROTATION_LABEL:
                    RotationSpecs rotationSpecs = (RotationSpecs) this.parseYamlObject(rule.getSpecs(), RotationSpecs.class);
                    newSample = this.applyRotation(newSample, rule.getFeature(), rotationSpecs);
                    break;
                case NORMALIZATION_LABEL:
                    NormalizationSpecs normalizationSpecs = (NormalizationSpecs) this.parseYamlObject(rule.getSpecs(), NormalizationSpecs.class);
                    newSample = this.applyNormalization(newSample, rule.getFeature(), normalizationSpecs);
                    break;
                case SUPPRESSION_LABEL:
                    newSample = this.applySuppression(newSample, rule.getFeature());
                    break;
                case PSEUDONYMIZATION_LABEL:
                    PseudonymizationSpecs pseudonymizationSpecs = (PseudonymizationSpecs) this.parseYamlObject(rule.getSpecs(), PseudonymizationSpecs.class);
                    newSample = this.applyPseudonymization(newSample, rule.getFeature(), pseudonymizationSpecs);
                    break;
                default:
                    throw new RuleNotRecognized();
            }
        }
        
        return Flux.just(Sample.newBuilder(newSample)
            .setState(States.ANONYMIZED)
            .build());
    }

    /**
     * This method parses a YAML configuration file and returns a ConfigYaml object.
     *
     * @param path The path to the YAML configuration file.
     *
     * @return A ConfigYaml object that represents the configuration defined in the YAML file.
    */
    protected ConfigYaml parseYamlFile(String path) {
        ObjectMapper om = new ObjectMapper(new YAMLFactory());

        try {
            File file = new File(path);
            return om.readValue(file, ConfigYaml.class);

        } catch (IOException e) {
            throw new YamlBadFormatted();
        }
    }

    /**
     * This method parses a YAML object and returns an instance of a specified class.
     *
     * @param specs The YAML object to be parsed. This is an instance of the Object class.
     * @param schema The class that the YAML object should be mapped to.
     *
     * @return An instance of the specified class that represents the parsed YAML object.
    */
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

    /**
     * Applies rotation anonymization to the sample.
     *
     * @param sample The sample to anonymize.
     * @param feature The feature to apply rotation on.
     * @param specs The rotation specifications.
     * @return The anonymized sample.
     */
    protected Sample applyRotation(Sample sample, String feature, RotationSpecs specs) {
        List<Float> newValues1 = new LinkedList<>();
        List<Float> newValues2 = new LinkedList<>();
        
        if (sample.getFloatDataMap().containsKey(feature) &&
            sample.getFloatDataMap().containsKey(specs.getFeature())) {
        
            List<Float> values1 = sample.getFloatDataMap().get(feature).getElementList();
            List<Float> values2 = sample.getFloatDataMap().get(specs.getFeature()).getElementList();

            if (values1.size() != values2.size()) {
                throw new RuleBadFormatted("The two features have different number of elements.");
            } else {
                for (int i = 0; i <  values1.size(); i++) {
                    float value1 = values1.get(i);
                    float value2 = values2.get(i);
                    float theta = specs.getTheta();

                    float newValue1 = (float) (value2*Math.sin(theta) + value1*Math.cos(theta));
                    float newValue2 = (float) (-value1*Math.sin(theta) + value2*Math.cos(theta));
                    newValues1.add(newValue1);
                    newValues2.add(newValue2);
                }
            }

            return Sample.newBuilder(sample)
                .putFloatData(feature, FloatArray.newBuilder().addAllElement(newValues1).build())
                .putFloatData(specs.getFeature(), FloatArray.newBuilder().addAllElement(newValues2).build())
                .build();
        }
        else {
            log.error(featureNotFound(feature, ROTATION_LABEL));
            return sample;
        }
    }

    /**
     * Applies normalization anonymization to the sample.
     *
     * @param sample The sample to anonymize.
     * @param feature The feature to apply normalization on.
     * @param specs The normalization specifications.
     * @return The anonymized sample.
    */
    protected Sample applyNormalization(Sample sample, String feature, NormalizationSpecs specs) {
        List<Float> newValues = new LinkedList<>();

        if (sample.getFloatDataMap().containsKey(feature)) {
            List<Float> values = sample.getFloatDataMap().get(feature).getElementList();
            for (Float value : values) {
                float newValue = (value - specs.getMean())/specs.getStd();
                newValues.add(newValue);
            }

            return Sample.newBuilder(sample)
            .putFloatData(feature, FloatArray.newBuilder().addAllElement(newValues).build())
            .build();
        } else {
            log.error(featureNotFound(feature, NORMALIZATION_LABEL));
            return sample;
        }
    }

    /**
     * Applies suppression anonymization to the sample.
     *
     * @param sample The sample to anonymize.
     * @param feature The feature to suppress.
     * @return The anonymized sample.
    */
    protected Sample applySuppression(Sample sample, String feature) {
        
        if (sample.getFloatDataMap().containsKey(feature)) {
            return Sample.newBuilder(sample)
                .removeFloatData(feature)
                .build();
        } else if (sample.getStringDataMap().containsKey(feature)) {
            return Sample.newBuilder(sample)
                .removeStringData(feature)
                .build();
        } else if (sample.getBoolDataMap().containsKey(feature)) {
            return Sample.newBuilder(sample)
                .removeBoolData(feature)
                .build();
        } else {
            log.error(featureNotFound(feature, SUPPRESSION_LABEL));
            return sample;
        }
    }

    /**
     * Applies pseudonymization anonymization to the sample.
     *
     * @param sample The sample to anonymize.
     * @param feature The feature to apply pseudonymization on.
     * @param specs The pseudonymization specifications.
     * @return The anonymized sample.
    */
    protected Sample applyPseudonymization(Sample sample, String feature, PseudonymizationSpecs specs) {

        List<String> featureNames = new LinkedList<>();
        featureNames.addAll(sample.getFloatDataMap().keySet());
        featureNames.addAll(sample.getStringDataMap().keySet());
        featureNames.addAll(sample.getBoolDataMap().keySet());
        featureNames = featureNames.stream().sorted().toList();

        List<String> features = List.of(feature);
        if (feature.equals("*")) {
            features = featureNames;
        }
        if (feature.startsWith("[")) {
            feature = feature.substring(1, feature.length());
            features = List.of(feature.split(","));
        }

        Sample newSample = Sample.newBuilder(sample).build();
        for (String field : features) {
            if (newSample.getFloatDataMap().containsKey(field)) {
                String newName = field;
                FloatArray newData = newSample.getFloatDataMap().get(field);

                if (specs.isFeatureName()) {
                    newName = String.format("X%d", featureNames.indexOf(field));
                }

                newSample = Sample.newBuilder(newSample)
                    .putFloatData(newName, newData)
                    .removeFloatData(field)
                    .build();

            } else if (newSample.getStringDataMap().containsKey(field)) {

                String newName = field;
                StringArray newData = newSample.getStringDataMap().get(field);
                
                if (specs.isFeatureName()) {
                    newName = String.format("X%d", featureNames.indexOf(field));
                }

                if(specs.isFeatureValue()) {
                    List<String> values = newData.getElementList()
                        .stream()
                        .map(v -> createHash(v, specs.getHashAlgorithm()))
                        .toList();
                    newData = StringArray.newBuilder().addAllElement(values).build();
                }
                
                newSample = Sample.newBuilder(newSample)
                    .putStringData(newName, newData)
                    .build();

                if (!newName.equals(field)) {
                    newSample = Sample.newBuilder(newSample).removeStringData(field).build();
                }

            } else if (newSample.getBoolDataMap().containsKey(field)) {
                String newName = field;
                BoolArray data = newSample.getBoolDataMap().get(field);

                if (specs.isFeatureName()) {
                    newName = String.format("X%d", featureNames.indexOf(field));
                }

                newSample = Sample.newBuilder(newSample)
                    .putBoolData(newName, data)
                    .removeBoolData(field)
                    .build();

            } else {
                log.error(featureNotFound(field, PSEUDONYMIZATION_LABEL));
            }

            if(newSample.getKey().equals(field)) {
                newSample = Sample.newBuilder(newSample).setKey(String.format("X%d", featureNames.indexOf(field))).build();
            }
        }

        return newSample;
    }

    private static String bytesToHex(byte[] hash) {
        // Convert bytes to hexadecimal string

        StringBuilder hexString = new StringBuilder(2 * hash.length);
        for (int i = 0; i < hash.length; i++) {
            String hex = Integer.toHexString(0xff & hash[i]);
            if(hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }

        return hexString.toString();
    }

    private String createHash(String name, String instance) {
        // Create hash value for the given name

        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance(instance);
            final byte[] hashBytes = digest.digest(name.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            log.error("Hash algorithm %s not recognized", instance);
            return name;
        }
    }

    private String featureNotFound(String feature, String rule) {
        // Handle feature not found error message
        
        return String.format("Feature %s not found for anonymization using rule %s", feature, rule);
    }
}
