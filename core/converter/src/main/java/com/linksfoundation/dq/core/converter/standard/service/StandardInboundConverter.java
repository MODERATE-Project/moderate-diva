package com.linksfoundation.dq.core.converter.standard.service;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonSyntaxException;
import com.linksfoundation.dq.core.converter.standard.model.DataCollectionModel;
import com.linksfoundation.dq.api.converter.service.ConverterManager;
import com.linksfoundation.dq.api.converter.service.InboundConverter;
import com.linksfoundation.dq.api.model.BoolArray;
import com.linksfoundation.dq.api.model.FloatArray;
import com.linksfoundation.dq.api.model.Sample;
import com.linksfoundation.dq.api.model.StringArray;
import com.linksfoundation.dq.api.model.Sample.States;
import com.linksfoundation.dq.api.utils.exceptions.MQTTMessageNotDeserializable;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * This class represents a StandardInboundConverter service that extends the InboundConverter.
 * It facilitates the conversion of incoming data into data quality samples.
 */
@Service
@Slf4j
public class StandardInboundConverter extends InboundConverter {

    protected StandardInboundConverter(ConverterManager manager) {
        super(manager);
    }

    /**
     * Converts the incoming sample data into samples.
     *
     * @param sample The incoming sample data to convert.
     * @return A Flux emitting the converted samples.
     * @throws MQTTMessageNotDeserializable if the incoming MQTT message cannot be deserialized.
    */
    public Flux<Sample> convertIn(Sample sample) {
        try {
            List<Sample> results = new LinkedList<>();

            Gson gson = new Gson();
            String json = sample.getStringDataMap().get("JSON").getElement(0);
            DataCollectionModel model = gson.fromJson(json, DataCollectionModel.class);
            String dataset = model.getDataItemID();
            if (dataset.contains("/")) {
                dataset = dataset.split("/")[4];
            }

            if (model.getMeasureUnit() != null) {
                if (model.getMeasureUnit().equals("List")) {
                    for (JsonElement element : model.getMetricValue().getAsJsonArray()) {
                        HashMap<String, Object> value = gson.fromJson(element.toString(), HashMap.class);
                        results.add(parseMetricValue(value, "", dataset, model.getMetricTypeID()));
                    }
                }

                if (model.getMeasureUnit().equals("Map")) {
                    HashMap<String, Object> element = gson.fromJson(model.getMetricValue().getAsJsonObject().toString(),
                            HashMap.class);
                    results.add(parseMetricValue(element, "", dataset, model.getMetricTypeID()));
                }

            } else {
                HashMap<String, Object> element = gson.fromJson(model.toString(), HashMap.class);
                results.add(parseMetricValue(element, "", dataset, model.getMetricTypeID()));
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

    /**
     * Parses the metric value and constructs a sample.
     *
     * @param metricValue The metric value to parse.
     * @param prefixItem The prefix item for nested values.
     * @param dataset The dataset associated with the sample.
     * @param key The key associated with the sample.
     * @return The parsed sample.
    */
    private Sample parseMetricValue(Map<String, Object> metricValue, String prefixItem, String dataset, String key) {

        Map<String, FloatArray.Builder> floatValues = new HashMap<>();
        Map<String, BoolArray.Builder> boolValues = new HashMap<>();
        Map<String, StringArray.Builder> stringValues = new HashMap<>();

        for (Map.Entry<String, Object> item : metricValue.entrySet()) {
            String itemKey = prefixItem.length() > 0 ? String.format("%s/%s", prefixItem, item.getKey())
                    : item.getKey();

            if (item.getValue() instanceof Map) {
                Sample nested = parseMetricValue((Map<String, Object>) item.getValue(), itemKey, dataset, key);

                nested.getFloatDataMap().entrySet().stream()
                        .forEach(e -> floatValues.put(e.getKey(), FloatArray.newBuilder(e.getValue())));
                nested.getBoolDataMap().entrySet().stream()
                        .forEach(e -> boolValues.put(e.getKey(), BoolArray.newBuilder(e.getValue())));
                nested.getStringDataMap().entrySet().stream()
                        .forEach(e -> stringValues.put(e.getKey(), StringArray.newBuilder(e.getValue())));
            } else {
                if (item.getValue() != null) {
                    String itemValue = item.getValue().toString();
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
        }

        return Sample.newBuilder()
                .setState(States.valueOf(manager.getInputState()))
                .clearFloatData()
                .clearBoolData()
                .clearStringData()
                .setDataset(dataset)
                .setKey(key)
                .putAllFloatData(floatValues.entrySet()
                        .stream().collect(Collectors.toMap(
                                Map.Entry::getKey,
                                e -> e.getValue().build())))
                .putAllBoolData(boolValues.entrySet()
                        .stream().collect(Collectors.toMap(
                                Map.Entry::getKey,
                                e -> e.getValue().build())))
                .putAllStringData(stringValues.entrySet()
                        .stream().collect(Collectors.toMap(
                                Map.Entry::getKey,
                                e -> e.getValue().build())))
                .build();
    }

    /**
     * Parses and adds a float value to the common map.
     *
     * @param field The field value to parse.
     * @param header The header associated with the field.
     * @param map The map to add the parsed float value.
     * @return true if the parsing and addition are successful, false otherwise.
    */
    private boolean parseFloat(String field, String header, Map<String, FloatArray.Builder> map) {
        boolean success = false;
        try {

            float value;
            if (field.equals("")) {
                return false;
            } else {
                value = Float.parseFloat(field);
            }

            if (map.containsKey(header)) {
                map.get(header).addElement(value);
            } else {
                map.put(header, FloatArray.newBuilder().addElement(value));
            }

            success = true;
        } catch (NumberFormatException e) {
            success = false;
        }

        return success;
    }

    /**
     * Parses and adds a boolean value to the map.
     *
     * @param field The field value to parse.
     * @param header The header associated with the field.
     * @param map The map to add the parsed boolean value.
     * @return true if the parsing and addition are successful, false otherwise.
    */
    private boolean parseBoolean(String field, String header, Map<String, BoolArray.Builder> map) {

        boolean success = false;
        Boolean value = null;
        if (field.equals("")) {
            return false;
        }
        if (field.equals("true") || field.equals("True")) {
            value = Boolean.TRUE;
        }
        if (field.equals("false") || field.equals("False")) {
            value = Boolean.FALSE;
        }

        if (value != null) {
            if (map.containsKey(header)) {
                map.get(header).addElement(value);
            } else {
                map.put(header, BoolArray.newBuilder().addElement(value));
            }

            success = true;
        }

        return success;
    }
}
