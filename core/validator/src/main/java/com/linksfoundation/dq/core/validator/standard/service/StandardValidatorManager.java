package com.linksfoundation.dq.core.validator.standard.service;

import com.linksfoundation.dq.api.utils.config.KafkaProducerConfig;
import com.linksfoundation.dq.api.utils.config.SampleConsumerConfig;
import com.linksfoundation.dq.api.validator.service.ValidationManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.reactive.ReactiveKafkaConsumerTemplate;
import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate;
import org.springframework.stereotype.Service;

/**
 * This class represents a StandardValidatorManager that extends the ValidationManager.
 * It is responsible for managing the validation process using Kafka messaging.
*/
@Service
@Slf4j
@Import({SampleConsumerConfig.class, KafkaProducerConfig.class})
public class StandardValidatorManager extends ValidationManager {
    public StandardValidatorManager(
            ReactiveKafkaConsumerTemplate<String, byte[]> sampleConsumer,
            ReactiveKafkaProducerTemplate<String, byte[]> producer) {
        super(sampleConsumer, producer);
    }
}

