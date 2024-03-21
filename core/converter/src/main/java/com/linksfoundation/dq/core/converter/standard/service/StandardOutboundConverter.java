package com.linksfoundation.dq.core.converter.standard.service;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import com.linksfoundation.dq.core.converter.standard.model.DataCollectionModel;
import com.linksfoundation.dq.api.converter.service.ConverterManager;
import com.linksfoundation.dq.api.converter.service.OutboundConverter;
import com.linksfoundation.dq.api.model.BoolArray;
import com.linksfoundation.dq.api.model.FloatArray;
import com.linksfoundation.dq.api.model.Sample;
import com.linksfoundation.dq.api.model.StringArray;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.ZonedDateTime;
import java.util.Map;

/**
 * This class represents a StandardOutboundConverter service that extends the OutboundConverter.
 * It facilitates the conversion of outgoing samples into JSON format.
*/
@Service
@Slf4j
public class StandardOutboundConverter extends OutboundConverter {

    protected StandardOutboundConverter(ConverterManager manager) {
        super(manager);
    }

    /**
     * Converts the outgoing sample into JSON format.
     *
     * @param sample The outgoing sample to convert.
     * @return A Flux emitting the converted sample.
    */
    public Flux<Sample> convertOut(Sample sample) {
        JsonObject obj = new JsonObject();

        for (Map.Entry<String, FloatArray> entry : sample.getFloatDataMap().entrySet()) {
            createEntryOrMap(obj, entry.getKey(), entry.getValue().getElement(0));
        }
        for (Map.Entry<String, StringArray> entry : sample.getStringDataMap().entrySet()) {
            createEntryOrMap(obj, entry.getKey(), entry.getValue().getElement(0));
        }
        for (Map.Entry<String, BoolArray> entry : sample.getBoolDataMap().entrySet()) {
            createEntryOrMap(obj, entry.getKey(), entry.getValue().getElement(0));
        }
        
        Gson gson = new Gson();
        String infoType = sample.getState().getValueDescriptor().getName().toLowerCase() + "Data";
        DataCollectionModel model = DataCollectionModel.builder()
                .timestamp(String.valueOf(System.currentTimeMillis()))
                .sourceID(sample.getMetadataMap().get("sourceID"))
                .sourceType(sample.getMetadataMap().get("sourceType"))
                .dataItemID(sample.getMetadataMap().get("dataItemID"))
                .dataType(sample.getMetadataMap().get("dataType"))
                .infoType(infoType)
                .measureUnit("Single")
                .metricTypeID(sample.getKey())
                .metricValue(obj)
                .build();

        String topic = String.format("%s/%s/%s/%s/%s",
                model.getSourceType(),
                model.getSourceID(),
                infoType,
                model.getDataType(),
                model.getDataItemID().startsWith("/") ? model.getDataItemID().substring(1) : model.getDataItemID()
        );

        Sample result = Sample.newBuilder()
                .setTs(sample.getTs())
                .setState(sample.getState())
                .setDataset(sample.getDataset())
                .putStringData("topic", StringArray.newBuilder()
                        .addElement(topic)
                        .build())
                .putStringData("JSON", StringArray.newBuilder()
                        .addElement(gson.toJson(model, DataCollectionModel.class))
                        .build())
                .build();

        return Flux.just(result);
    }

    /**
     * Creates an entry or nested map in the JSON object.
     *
     * @param obj The JSON object to populate.
     * @param key The key for the entry or map.
     * @param value The value to insert.
    */
    private void createEntryOrMap(JsonObject obj, String key, Object value) {
        String[] keys = key.split("/");
        if (keys.length > 1) {
            JsonObject nested = null;
            if (!obj.has(keys[0])) {
                nested = new JsonObject();
            } else {
                nested = obj.get(keys[0]).getAsJsonObject();
            }

            this.createEntryOrMap(nested, key.split("/")[1], value);
            obj.add(key.split("/")[0], nested);
        }
        else {
            JsonPrimitive primitive = null;
            if (value instanceof String) { primitive = new JsonPrimitive((String) value); }
            if (value instanceof Boolean) { primitive = new JsonPrimitive((Boolean) value); }
            if (value instanceof Float) { primitive = new JsonPrimitive((Float) value); }
            
            obj.add(key, primitive);
        }
    }
}
