from kafka import KafkaProducer, KafkaConsumer
from google.protobuf.json_format import MessageToDict
import ssl
from kafka.errors import KafkaError
from kafka.structs import TopicPartition

import core.proto.validation_pb2 as validation_message
import core.settings import settings

import os
import time

import logging 
logging.basicConfig(level=logging.INFO)

class KafkaCommunicationGateway:
    """This class is the only communication interface linked to Kafka of the backend
    architecture.
    It is able to send/receive simulation/ai messages to/from Kafka.
    In particular, when a message is received, the local statistics are immediately updated.

    """

    context = ssl.create_default_context()
    context.check_hostname = False
    context.verify_mode = ssl.CERT_NONE

    broker = settings.broker
    security_protocol = settings.security_protocol
    mechanism = settings.mechanism
    username = settings.sasl_username
    password = settings.sasl_password

    def __init__(self, name: str, topic: str):
        """It instanciates the consumer and producer for the specified `topic`.

        Args:
            topic (str): name of the topic.
        """

        self.consumer = self.config_consumer(name)
        partition = TopicPartition(topic, 0)
        self.consumer.assign([partition])
        self.topic = topic

    def config_consumer(self, name: str):
        """It returns the consumer configuration for the specified `topic`.

        Args:
            topic (str): name of the topic.

        Returns:
            KafkaConsumer: consumer object.
        """

        return KafkaConsumer(
            bootstrap_servers = self.broker,
            group_id = name,
            security_protocol = self.security_protocol,
            sasl_mechanism = self.mechanism,
            sasl_plain_username = self.username,
            sasl_plain_password = self.password,
            auto_offset_reset = "earliest",
            ssl_context = self.context,
            ssl_check_hostname = False
        )
    
    def receive(self):
        """It polls new messages from Kafka and it returns them.

        Returns:
            messages (list): list of messages read by the current poll.
        """

        ret = []
        msg_pack = self.consumer.poll(timeout_ms=5000)
        for _, messages in msg_pack.items():
            for message in messages:
                task = validation_message.Validation()
                task.ParseFromString(message.value)
                task = MessageToDict(task, including_default_value_fields=True)
                ret.append(
                    {
                        "timestamp": message.timestamp,
                        "headers": message.headers,
                        "message": task,
                    }
                )
                
        return ret

if __name__ == "__main__":
    kfg = KafkaCommunicationGateway()