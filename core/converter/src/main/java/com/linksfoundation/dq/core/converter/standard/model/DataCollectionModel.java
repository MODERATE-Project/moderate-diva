package com.linksfoundation.dq.core.converter.standard.model;

import com.google.gson.JsonElement;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

@Builder
@Jacksonized
@Getter
public class DataCollectionModel {
    String timestamp;
    String sourceType;
    String sourceID;
    String infoType;
    String dataType;
    String dataItemID;
    String metricTypeID;
    JsonElement metricValue;
    String measureUnit;
}
