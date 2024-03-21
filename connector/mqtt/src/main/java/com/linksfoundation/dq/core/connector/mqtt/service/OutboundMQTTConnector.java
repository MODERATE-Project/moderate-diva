package com.linksfoundation.dq.core.connector.mqtt.service;

import com.linksfoundation.dq.api.connector.service.ConnectorManager;
import com.linksfoundation.dq.api.connector.service.OutboundConnector;
import com.linksfoundation.dq.api.model.Sample;

import com.linksfoundation.dq.api.utils.exceptions.MQTTClientNotConnected;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.*;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;
import java.nio.charset.StandardCharsets;

/**
 * This class represents an OutboundMQTTConnector service that extends the OutboundConnector.
 * It facilitates the export of data quality samples via MQTT.
*/
@Service
@Slf4j
public class OutboundMQTTConnector extends OutboundConnector implements CommandLineRunner {

    @Value(value = "${MQTT_URI}")
    private String mqttUri;
    @Value(value = "${MQTT_CLIENT_ID}")
    private String mqttClientId;
    @Value(value = "${MQTT_USERNAME}")
    private String mqttUsername;
    @Value(value = "${MQTT_PASSWORD}")
    private String mqttPassword;
    @Value(value = "${MQTT_QOS}")
    private String mqttQos;
    private MqttClient mqttClient;

    protected OutboundMQTTConnector(ConnectorManager manager) {
        super(manager);
    }

    /**
     * Exports the given sample via MQTT.
     *
     * @param sample The sample to export.
    */
    @Override
    public void exportSamples(Sample sample) {
        String topic = sample.getStringDataMap().get("topic").getElement(0);
        String json = sample.getStringDataMap().get("JSON").getElement(0);

        MqttMessage message = new MqttMessage();
        message.setQos(Integer.parseInt(mqttQos));
        message.setPayload(json.getBytes(StandardCharsets.UTF_8));

        try {
            mqttClient.publish(topic, message);
        } catch (MqttException e) {
            log.error("Message not published.");
        }
    }

    /**
     * Runs the OutboundMQTTConnector service.
     *
     * @param args The command-line arguments passed to the application.
     * @throws MQTTClientNotConnected if the MQTT client fails to connect.
    */
    public void run(String... args) {
        try {
            mqttClient = new MqttClient(mqttUri, mqttClientId + "-out");
            MqttConnectOptions options = new MqttConnectOptions();

            if (mqttUsername.length() > 0 && mqttPassword.length() > 0) {
                options.setUserName(mqttUsername);
                options.setPassword(mqttPassword.toCharArray());
            }

            mqttClient.connect(options);
            log.info("Starting Exporting");
            this.receive().subscribe();

        } catch (MqttException e) {
            throw new MQTTClientNotConnected(e.toString());
        }
    }
}
