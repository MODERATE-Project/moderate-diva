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

@Service
@Slf4j
public class StandardOutboundConverter extends OutboundConverter {

    protected StandardOutboundConverter(ConverterManager manager) {
        super(manager);
    }

    public Flux<Sample> convertOut(Sample sample) {
        JsonObject obj = new JsonObject();

        for (Map.Entry<String, FloatArray> entry : sample.getFloatDataMap().entrySet()) {
            obj.add(entry.getKey(), new JsonPrimitive(entry.getValue().getElement(0)));
        }
        for (Map.Entry<String, StringArray> entry : sample.getStringDataMap().entrySet()) {
            obj.add(entry.getKey(), new JsonPrimitive(entry.getValue().getElement(0)));
        }
        for (Map.Entry<String, BoolArray> entry : sample.getBoolDataMap().entrySet()) {
            obj.add(entry.getKey(), new JsonPrimitive(entry.getValue().getElement(0)));
        }

        String infoType = sample.getState().getValueDescriptor().getName().toLowerCase() + "Data";
        DataCollectionModel model = DataCollectionModel.builder()
                .timestamp(ZonedDateTime.now().toString())
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
                model.getDataItemID()
        );

        Gson gson = new Gson();
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
}
