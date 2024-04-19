from aiokafka import AIOKafkaConsumer, AIOKafkaProducer
from utils.settings import settings

import ssl
import certifi

"""It creates a Kafka Producer

Parameters:
    topic (str): topic to subscribe to

Returns:
    AIOKafkaProducer: Kafka async producer object
"""
def create_producer() -> AIOKafkaProducer:
    context = ssl.create_default_context()
    context.load_verify_locations(cafile=settings.ssl_cafile)
    
    return AIOKafkaProducer(
        bootstrap_servers=settings.bootstrap_servers,
        security_protocol = settings.security_protocol,
        sasl_mechanism = settings.sasl_mechanism,
        sasl_plain_username = settings.sasl_plain_username,
        sasl_plain_password = settings.sasl_plain_password,
        ssl_context = context,
        key_serializer=lambda v:v.encode(),
        value_serializer=lambda v: v
    )
    
"""It creates a consumer for the conncector topic
"""
def create_connector_consumer() -> AIOKafkaConsumer:
    return create_consumer(settings.connector_topic)