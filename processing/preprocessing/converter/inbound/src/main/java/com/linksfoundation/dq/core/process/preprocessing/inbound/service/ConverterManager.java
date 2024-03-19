package com.linksfoundation.dq.core.process.preprocessing.inbound.service;

import com.linksfoundation.dq.api.processing.preprocessing.service.PreprocessingManager;
import com.linksfoundation.dq.api.utils.config.KafkaProducerConfig;
import com.linksfoundation.dq.api.utils.config.SampleConsumerConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.reactive.ReactiveKafkaConsumerTemplate;
import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@Import({SampleConsumerConfig.class, KafkaProducerConfig.class})
public class ConverterManager extends PreprocessingManager {
    public ConverterManager(
            ReactiveKafkaConsumerTemplate<String, byte[]> sampleConsumer,
            ReactiveKafkaProducerTemplate<String, byte[]> sampleProducer) {
        super(sampleConsumer, sampleProducer);
    }
}
