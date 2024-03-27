from aiokafka import AIOKafkaConsumer, AIOKafkaProducer
from utils.settings import settings

import ssl

"""It creates a Kafka Consumer

Parameters:
    topic (str): topic to subscribe to

Returns:
    AIOKafkaConsumer: Kafka async consumer object
"""
def create_consumer(topic: str) -> AIOKafkaConsumer:
    
    context = ssl.create_default_context()
    context.check_hostname = False
    context.verify_mode = ssl.CERT_NONE    
    
    return AIOKafkaConsumer(
        topic,
        bootstrap_servers=settings.bootstrap_servers,
        group_id=settings.group_id,
        security_protocol = settings.security_protocol,
        sasl_mechanism = settings.sasl_mechanism,
        sasl_plain_username = settings.sasl_plain_username,
        sasl_plain_password = settings.sasl_plain_password,
        auto_offset_reset = "earliest",
        ssl_context = context,
    )

"""It creates a Kafka Producer

Parameters:
    topic (str): topic to subscribe to

Returns:
    AIOKafkaProducer: Kafka async producer object
"""
def create_producer() -> AIOKafkaProducer:
    context = ssl.create_default_context()
    context.check_hostname = False
    context.verify_mode = ssl.CERT_NONE  
    
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

"""It creates a consumer for the sample topic
"""
def create_sample_consumer() -> AIOKafkaConsumer:
    return create_consumer(settings.sample_topic)