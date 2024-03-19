package com.linksfoundation.dq.core.connector.mqtt.service;

import com.linksfoundation.dq.api.connector.service.InboundConnector;
import com.linksfoundation.dq.api.utils.exceptions.MQTTClientNotConnected;
import com.linksfoundation.dq.core.connector.mqtt.model.MQTTSample;
import com.linksfoundation.dq.api.model.Sample;
import com.linksfoundation.dq.api.model.StringArray;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.nio.charset.StandardCharsets;
import java.util.List;


@Service
@Slf4j
public class InboundMQTTConnector extends InboundConnector implements CommandLineRunner, MqttCallback {

    @Value(value = "${MQTT_URI}")
    private String mqttUri;
    @Value(value = "${MQTT_CLIENT_ID}")
    private String mqttClientId;
    @Value(value = "${MQTT_USERNAME}")
    private String mqttUsername;
    @Value(value = "${MQTT_PASSWORD}")
    private String mqttPassword;
    @Value(value = "${MQTT_INPUT_TOPIC}")
    private String mqttTopic;
    @Value(value = "${MQTT_QOS}")
    private String mqttQos;

    private Sinks.Many<MQTTSample> samplesOut = Sinks.many().replay().all();

    public InboundMQTTConnector(MQTTConnectorManager manager) {
        super(manager);
    }

    @Override
    protected Flux<Sample> importSamples() {
        return samplesOut
                .asFlux()
                .flatMap(this::parseMQTTMessage)
                .doOnNext(sample -> this.manager.getSamplesIn().emitNext(sample, Sinks.EmitFailureHandler.FAIL_FAST));
    }

    private Flux<Sample> parseMQTTMessage(MQTTSample message) {

        Sample sample = Sample.newBuilder()
                .setTs(System.currentTimeMillis())
                .setState(Sample.States.RAW)
                .putStringData("topic", StringArray
                        .newBuilder()
                        .addElement(message.getTopic())
                        .build())
                .putStringData("JSON", StringArray
                        .newBuilder()
                        .addElement(new String(message.getPayload(), StandardCharsets.UTF_8))
                        .build())
                .build();

        return Flux.fromIterable(List.of(sample));
    }

    @Override
    public void run(String... args) {

        try {
            MqttClient mqttClient = new MqttClient(mqttUri, mqttClientId + "-in");
            MqttConnectOptions options = new MqttConnectOptions();

            if (mqttUsername.length() > 0 && mqttPassword.length() > 0) {
                options.setUserName(mqttUsername);
                options.setPassword(mqttPassword.toCharArray());
            }
            mqttClient.connect(options);

            mqttClient.setCallback(this);
            mqttClient.subscribe(mqttTopic, Integer.parseInt(mqttQos));

        } catch (MqttException e) {
            throw new MQTTClientNotConnected();
        }

        log.info("Starting Importing");
        this.importSamples().subscribe();
    }

    @Override
    public void connectionLost(Throwable cause) {}

    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        MQTTSample sample = MQTTSample
                .builder()
                .topic(topic)
                .payload(message.getPayload())
                .build();

        this.samplesOut.emitNext(sample, Sinks.EmitFailureHandler.FAIL_FAST);
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {}
}
