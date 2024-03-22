##################################
########## GRADLE BUILD ##########
##################################

FROM gradle:7.6.4-jdk17 AS build

ARG DQ_API_TOKEN
ARG DQ_CORE_TOKEN

COPY --chown=gradle:gradle . /home/gradle/
WORKDIR /home/gradle/

RUN echo "dq_api_token=${DQ_API_TOKEN}" >> gradle.properties
RUN echo "dq_core_token=${DQ_CORE_TOKEN}" >> gradle.properties

RUN gradle build --no-daemon
RUN gradle publish

##################################
############ APP COPY ############
##################################

FROM eclipse-temurin:17-jre-alpine

LABEL maintainer="nicolo.bertozzi@linksfoundation.com"
LABEL org.label-schema.schema-version = "1.0"
LABEL org.label-schema.vendor = "LINKS Foundation, Turin, Italy"
LABEL org.label-schema.description = "Validator Module"

WORKDIR /app
COPY --from=build /home/gradle/build/libs/validator-1.0-SNAPSHOT.jar /app/validator-1.0-SNAPSHOT.jar

ENV KAFKA_BOOTSTRAP_SERVERS=""
ENV KAFKA_SASL_USERNAME=""
ENV KAFKA_SASL_PASSWORD=""
ENV KAFKA_TRUSTSTORE_LOCATION=""
ENV KAFKA_TRUSTSTORE_PASSWORD=""
ENV KAFKA_GROUP_ID=""
ENV KAFKA_ENDPOINT_IDENTIFICATION=""

ENV MQTT_URI=""
ENV MQTT_CLIENT_ID=""
ENV MQTT_USERNAME=""
ENV MQTT_PASSWORD=""
ENV MQTT_INPUT_TOPIC=""
ENV MQTT_QOS=""

ENV SAMPLE_TOPIC=""
ENV SAMPLE_PARTITION=""
ENV VALIDATION_TOPIC=""
ENV DATASET_NAMES=""
ENV CONFIG_FILE=""

ENTRYPOINT ["java", "-jar", "-Dspring.profiles.active=prod", "/app/validator-1.0-SNAPSHOT.jar"]