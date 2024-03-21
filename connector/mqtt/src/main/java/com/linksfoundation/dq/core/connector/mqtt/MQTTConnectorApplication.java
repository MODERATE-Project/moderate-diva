package com.linksfoundation.dq.core.connector.mqtt;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * This is the main class for the MQTTConnectorApplication.
 * The main() method uses Spring Boot's SpringApplication.run() method to launch an application.
*/
@SpringBootApplication
public class MQTTConnectorApplication {
	public static void main(String[] args) {SpringApplication.run(MQTTConnectorApplication.class, args);}
}
