import asyncio
import functools

from aiokafka import ConsumerRecord
from model.message_key import MessageKey

from utils.kafka import create_connector_consumer, create_producer
from utils.queues import connector_queue
from utils.checks import parse_message, check_state, check_dataset
from utils.settings import settings

from serde.sample_pb2 import Sample

from aiologger import Logger
from aiologger.formatters.base import Formatter
formatter = Formatter('[%(asctime)s][%(module)s][%(levelname)s]: %(message)s')
logger = Logger.with_default_handlers(formatter=formatter)

"""It consumes the messages received from the connector topic

Parameters:
    item (Sample): the sample from which the method has to build the message key

Returns:
    key (str): JSON formatted key
"""     
async def create_key(item):
    import time
    
    if (item.state == Sample.States.NEW):
        ns = time.time_ns()//10**3
        item.ts = ns
        key = MessageKey(
            dataset=item.dataset,
            timestamp=ns
        )
    else:
        key = MessageKey(
            dataset=item.dataset,
            timestamp=item.ts
        )
    
    return key.json()
       
"""It publishes messages into the connector topic
""" 
async def produce_connector():
    producer = create_producer()
    await producer.start()
    await logger.info("Connector Producer STARTED")
    
    try:
        while True:
            item = await connector_queue.get()
            key = await create_key(item)
            await producer.send_and_wait(
                topic=settings.connector_topic, 
                key=key,
                value=item.SerializeToString(),
                partition=settings.connector_partition
            )
            
            await logger.info(f"Published Connector Message, ts: {item.ts}")
    finally:
        await producer.stop()
        
async def start():
    logger.info("Manager STARTED")
    
    await asyncio.gather(
        produce_connector()
    )