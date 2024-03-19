package com.linksfoundation.dq.core.connector.mqtt.model;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class MQTTSample {
    String topic;
    byte[] payload;
}
