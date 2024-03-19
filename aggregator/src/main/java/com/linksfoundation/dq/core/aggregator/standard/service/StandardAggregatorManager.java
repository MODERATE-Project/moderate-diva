package com.linksfoundation.dq.core.aggregator.standard.service;

import com.linksfoundation.dq.api.utils.config.KafkaProducerConfig;
import com.linksfoundation.dq.api.utils.config.SampleConsumerConfig;
import com.linksfoundation.dq.api.aggregator.service.AggregatorManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.reactive.ReactiveKafkaConsumerTemplate;
import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@Import({SampleConsumerConfig.class, KafkaProducerConfig.class})
public class StandardAggregatorManager extends AggregatorManager {
    public StandardAggregatorManager(
            ReactiveKafkaConsumerTemplate<String, byte[]> sampleConsumer,
            ReactiveKafkaProducerTemplate<String, byte[]> producer) {
        super(sampleConsumer, producer);
    }
}

