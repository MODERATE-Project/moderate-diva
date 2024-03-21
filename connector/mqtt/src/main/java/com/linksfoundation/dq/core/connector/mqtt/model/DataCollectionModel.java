package com.linksfoundation.dq.core.connector.mqtt.model;

import com.google.gson.JsonElement;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

/**
 * This is the model adopted by the messages received using the connector.
*/
@Builder
@Jacksonized
@Getter
public class DataCollectionModel {
    String timestamp;
    String originalTimestamp;
    String sourceType;
    String sourceID;
    String infoType;
    String dataType;
    String dataItemID;
    String metricTypeID;
    JsonElement metricValue;
    String measureUnit;
}
