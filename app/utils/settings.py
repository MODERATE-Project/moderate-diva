from dataclasses import dataclass
import os

@dataclass
class Settings:
    
    bootstrap_servers: str = os.getenv("KAFKA_BOOTSTRAP_SERVERS") or "csc-ai.csc-ai.csc:9092"
    group_id: str = os.getenv("KAFKA_GROUP_ID") or "moderate"
    security_protocol: str = "SASL_SSL"
    sasl_mechanism: str = "PLAIN"
    sasl_plain_username: str = os.getenv("KAFKA_SASL_USERNAME") or "kafka-dq"
    sasl_plain_password: str = os.getenv("KAFKA_SASL_PASSWORD") or "KAFKA!dq!2023"
    ssl_cafile: str = os.getenv("KAFKA_SSL_CAFILE") or "utils/ca.crt"
    
    connector_topic: str = os.getenv("CONNECTOR_TOPIC") or "connector"
    connector_partition: str = int(os.getenv("CONNECTOR_PARTITION") or "0")
    input_state: str = os.getenv("INPUT_STATE") or "RAW"
    output_state: str = os.getenv("OUTPUT_STATE") or "VALID,ANONYMIZED"
    dataset_names: str = os.getenv("DATASET_NAMES") or "moderate/lombardia"

    auth_url: str = os.getenv("AUTH_URL") or "https://api.gw.moderate.cloud/api/token"
    auth_user: str = os.getenv("AUTH_USER") or "nicolo"
    auth_password: str = os.getenv("AUTH_PASS") or "MODERATE_Links!2024!"

    asset_url: str = os.getenv("ASSET_URL") or "https://api.gw.moderate.cloud/asset"

    polling_time: int = int(os.getenv("POLLING_TIME") or "60")
    
settings = Settings()