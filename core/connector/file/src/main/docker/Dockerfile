FROM openjdk:17

LABEL maintainer="nicolo.bertozzi@linksfoundation.com"
LABEL org.label-schema.schema-version = "1.0"
LABEL org.label-schema.vendor = "LINKS Foundation, Turin, Italy"
LABEL org.label-schema.description = "FileConnectorModule"

WORKDIR /app
COPY ./file-1.0-SNAPSHOT.jar /app

ENV KAFKA_BOOTSTRAP_SERVERS=""
ENV KAFKA_SASL_USERNAME=""
ENV KAFKA_SASL_PASSWORD=""
ENV KAFKA_TRUSTSTORE_LOCATION=""
ENV KAFKA_TRUSTSTORE_PASSWORD=""
ENV KAFKA_GROUP_ID=""
ENV SAMPLE_TOPIC=""
ENV SAMPLE_PARTITION=""
ENV DATASET_NAME=""
ENV INPUT_FILE=""
ENV OUTPUT_FILE=""
ENV SAMPLE_STATE=""
ENV N_ROWS=""

ENTRYPOINT ["java","-jar", "-Dspring.profiles.active=prod", "/app/file-1.0-SNAPSHOT.jar"]