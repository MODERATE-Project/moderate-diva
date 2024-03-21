package com.linksfoundation.dq.core.processing.anonymization.standard;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * This is the main class for the StandardAnonymizationApplication.
 * The main() method uses Spring Boot's SpringApplication.run() method to launch an application.
*/
@SpringBootApplication
public class StandardAnonymizationApplication {
    public static void main(String[] args) {
        SpringApplication.run(StandardAnonymizationApplication.class, args);}
}