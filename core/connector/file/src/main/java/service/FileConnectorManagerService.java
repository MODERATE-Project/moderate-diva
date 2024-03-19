package service;

import com.linksfoundation.dq.connectors.service.ConnectorManagerService;
import com.linksfoundation.dq.utils.config.KafkaProducerConfig;
import com.linksfoundation.dq.utils.config.SampleConsumerConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.reactive.ReactiveKafkaConsumerTemplate;
import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@Import({SampleConsumerConfig.class, KafkaProducerConfig.class})
public class FileConnectorManagerService extends ConnectorManagerService {

    public FileConnectorManagerService(ReactiveKafkaProducerTemplate<String, byte[]> sampleProducer, ReactiveKafkaConsumerTemplate<String, byte[]> sampleConsumer) {
        super(sampleProducer, sampleConsumer);
    }
}
