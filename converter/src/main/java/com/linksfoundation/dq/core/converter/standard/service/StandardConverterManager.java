package com.linksfoundation.dq.core.converter.standard.service;

import com.linksfoundation.dq.api.utils.config.ConnectorConsumerConfig;
import com.linksfoundation.dq.api.utils.config.KafkaProducerConfig;
import com.linksfoundation.dq.api.utils.config.SampleConsumerConfig;
import com.linksfoundation.dq.api.converter.service.ConverterManager;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.reactive.ReactiveKafkaConsumerTemplate;
import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@Import({ConnectorConsumerConfig.class, SampleConsumerConfig.class, KafkaProducerConfig.class})
public class StandardConverterManager extends ConverterManager
{
    public StandardConverterManager(
            @Qualifier(value = "connectorConsumerTemplate") ReactiveKafkaConsumerTemplate<String, byte[]> connectorConsumer,
            @Qualifier(value = "sampleConsumerTemplate") ReactiveKafkaConsumerTemplate<String, byte[]> sampleConsumer,
            ReactiveKafkaProducerTemplate<String, byte[]> sampleProducer) {
        super(connectorConsumer, sampleConsumer, sampleProducer);
    }
}
