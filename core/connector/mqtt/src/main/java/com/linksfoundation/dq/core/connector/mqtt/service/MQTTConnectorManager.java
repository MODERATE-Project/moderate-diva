package com.linksfoundation.dq.core.connector.mqtt.service;

import com.linksfoundation.dq.api.connector.service.ConnectorManager;
import com.linksfoundation.dq.api.utils.config.KafkaProducerConfig;
import com.linksfoundation.dq.api.utils.config.SampleConsumerConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.reactive.ReactiveKafkaConsumerTemplate;
import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate;
import org.springframework.stereotype.Service;

/**
 * This class represents a StandardAggregatorManager that extends the AggregatorManager.
 * It is responsible for managing the data gathering process using Kafka messaging and MQTT.
*/
@Service
@Slf4j
@Import({SampleConsumerConfig.class, KafkaProducerConfig.class})
public class MQTTConnectorManager extends ConnectorManager {

    public MQTTConnectorManager(
            ReactiveKafkaProducerTemplate<String, byte[]> sampleProducer,
            ReactiveKafkaConsumerTemplate<String, byte[]> sampleConsumer) {
        super(sampleProducer, sampleConsumer);
    }
}
