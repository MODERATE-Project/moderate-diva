package com.linksfoundation.dq.core.connector.mqtt.model;

import lombok.Builder;
import lombok.Getter;

/**
 * Support data model used to share data between the services and the MQTTConnectorManager.
*/
@Builder
@Getter
public class MQTTSample {
    String topic;
    byte[] payload;
}
