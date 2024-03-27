import asyncio
import functools

from aiokafka import ConsumerRecord
from model.message_key import MessageKey

from utils.kafka import create_connector_consumer, create_sample_consumer, create_producer
from utils.queues import inbound_queue, outbound_queue, sample_queue, connector_queue
from utils.checks import parse_message, check_state, check_dataset
from utils.settings import settings

from serde.sample_pb2 import Sample

from aiologger import Logger
from aiologger.formatters.base import Formatter
formatter = Formatter('[%(asctime)s][%(module)s][%(levelname)s]: %(message)s')
logger = Logger.with_default_handlers(formatter=formatter)

def log(topic: str):
    def wrapper(func):
        @functools.wraps(func)
        async def wrapped(msg: ConsumerRecord):
            await logger.info(f"Received {topic} Message, offset {msg.offset}")
            return await func(msg)
        return wrapped
    return wrapper

@log(topic = "Connector")
@check_dataset(datasets = settings.dataset_names)
@parse_message(serde = Sample)
@check_state(states = settings.input_state)
async def parse_and_check_connector(msg):
    if msg is not None:
        await inbound_queue.put(msg)

@log(topic = "Sample")
@parse_message(serde = Sample)
@check_state(states = settings.output_state)
async def parse_and_check_sample(msg):
    if msg is not None:
        await outbound_queue.put(msg)

"""It consumes the messages received from the connector topic
"""
async def consume_connector():
    consumer = create_connector_consumer()
    await consumer.start()
    await logger.info("Connector Consumer STARTED")
    
    try:
        async for msg in consumer:
            await parse_and_check_connector(msg)
    finally:
        await consumer.stop()
    
"""It consumes the messages received from the sample topic
"""    
async def consume_sample():
    consumer = create_sample_consumer()
    await consumer.start()
    await logger.info("Sample Consumer STARTED")
    
    try:
        async for msg in consumer:
            await parse_and_check_sample(msg)
    finally:
        await consumer.stop()

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

"""It publishes messages into the sample topic
"""
async def produce_sample():
    producer = create_producer()
    await producer.start()
    await logger.info("Sample Producer STARTED")
    
    try:
        while True:
            item = await sample_queue.get()
            key = await create_key(item)
            await producer.send_and_wait(
                topic=settings.sample_topic, 
                key=key,
                value=item.SerializeToString(),
                partition=settings.sample_partition
            )
            
            await logger.info(f"Published Sample Message, ts: {item.ts}")
    finally:
        await producer.stop()
       
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
                partition=settings.sample_partition
            )
            
            await logger.info(f"Published Connector Message, ts: {item.ts}")
    finally:
        await producer.stop()
        
async def start():
    logger.info("Manager STARTED")
    
    await asyncio.gather(
        consume_connector(),
        consume_sample(),
        produce_sample(),
        produce_connector()
    )