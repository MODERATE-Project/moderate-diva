from dataclasses import dataclass
import os

@dataclass
class Settings:
    
    bootstrap_servers: str = os.getenv("KAFKA_BOOTSTRAP_SERVERS") or "localhost:9092"
    group_id: str = os.getenv("KAFKA_GROUP_ID") or "localhost:9092"
    connector_topic: str = os.getenv("CONNECTOR_TOPIC") or "connector"
    sample_topic: str =  os.getenv("SAMPLE_TOPIC") or "sample"
    sample_partition: str = int(os.getenv("SAMPLE_PARTITION") or "0")
    security_protocol: str = "SASL_SSL"
    sasl_mechanism: str = "PLAIN"
    sasl_plain_username: str = os.getenv("KAFKA_SASL_USERNAME") or "kafka-dq"
    sasl_plain_password: str = os.getenv("KAFKA_SASL_PASSWORD") or "KAFKA!dq!2023"
    input_state: str = os.getenv("INPUT_STATE") or "RAW"
    output_state: str = os.getenv("OUTPUT_STATE") or "VALID,ANONYMIZED"
    dataset_names: str = os.getenv("DATASET_NAMES") or "moderate/lombardia"
    
settings = Settings()