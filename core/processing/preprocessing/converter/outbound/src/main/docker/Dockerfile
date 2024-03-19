FROM eclipse-temurin:17-jre-alpine

LABEL maintainer = "nicolo.bertozzi@linksfoundation.com"
LABEL org.label-schema.schema-version = "1.0"
LABEL org.label-schema.vendor = "LINKS Foundation, Turin, Italy"
LABEL org.label-schema.description = "Outbound Converter Module"

WORKDIR /app
COPY ./outbound-1.0-SNAPSHOT.jar /app

ENV KAFKA_BOOTSTRAP_SERVERS=""
ENV KAFKA_SASL_USERNAME=""
ENV KAFKA_SASL_PASSWORD=""
ENV KAFKA_TRUSTSTORE_LOCATION=""
ENV KAFKA_TRUSTSTORE_PASSWORD=""
ENV KAFKA_GROUP_ID=""

ENV MQTT_URI=""
ENV MQTT_CLIENT_ID=""
ENV MQTT_USERNAME=""
ENV MQTT_PASSWORD=""
ENV MQTT_INPUT_TOPIC=""
ENV MQTT_QOS=""

ENV SAMPLE_TOPIC=""
ENV SAMPLE_PARTITION=""
ENV SAMPLE_STATE="VALID,ANONYMIZED"
ENV PREPROCESSED_TOPIC=""
ENV DATASET_NAME=""

ENTRYPOINT ["java","-jar", "-Dspring.profiles.active=prod", "/app/outbound-1.0-SNAPSHOT.jar"]