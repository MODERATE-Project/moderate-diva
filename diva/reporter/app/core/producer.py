from kafka import KafkaConsumer, KafkaProducer
from datetime import datetime
import ssl
import time

import core.proto.simulation_task_pb2 as simulation_task
from core.proto.simulation_task_pb2 import SimulationTask
import core.proto.ai_task_pb2 as ai_task

import logging
logging.basicConfig(level=logging.INFO)

"""
TESTING FILE AND CLASS
"""
class KafkaCommunicationGateway:
    producer = None

    def __init__(self):
        self.producer = self.config_producer()

    def config_producer(self):
        
        broker = "maestri.ismb.it:9093"
        cert = "../../config/localhost.crt"
        key = "../../config/localhost.key"
        password = "123456"
        
        context = ssl.create_default_context()
        context.load_cert_chain(certfile=cert, keyfile=key, password=password)
        context.check_hostname = False
        context.verify_mode = ssl.CERT_NONE

        security_protocol = "SSL"
        api_version = (0, 11, 2)

        return KafkaProducer(
            bootstrap_servers = broker,
            client_id = "GUI",
            security_protocol = security_protocol,
            ssl_context = context,
            api_version = api_version,
            key_serializer = lambda x: x.encode(),
            value_serializer = lambda x: x.SerializeToString()
        )
    
    def send(self, task: ai_task.AITask, headers: list):
        future = self.producer.send("ai", value=task, key="tts", headers=headers)
        try:
            _ = future.get(timeout=10)
        except Exception as e:
            print(e)

if __name__ == "__main__":
    kfg = KafkaCommunicationGateway()
    time.sleep(3)
    task = simulation_task.SimulationTask()
    task.state = simulation_task.SimulationTask.COMPLETED

    headers = [
        ("source", str.encode("sim")), 
        ("destination", str.encode("gui")),
        ("correlationTs", str.encode("1666276969088482900"))
    ]    
    """

    task = ai_task.AITask()
    task.state = ai_task.AITask.PLANNED

    headers = [
        ("source", str.encode("sim")), 
        ("destination", str.encode("ai")),
        ("correlationTs", str.encode("1666276969088482900"))
    ]

    kfg.send(task, headers=headers)
    """