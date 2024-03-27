import functools

import json
from aiokafka import ConsumerRecord

from serde.sample_pb2 import Sample
from model.message_key import MessageKey 

from aiologger import Logger
from aiologger.formatters.base import Formatter
from aiologger.levels import LogLevel
formatter = Formatter('[%(asctime)s][%(module)s][%(levelname)s]: %(message)s')
logger = Logger.with_default_handlers(formatter=formatter, level=LogLevel.DEBUG)

"""This annotation verifies if the dataset of the message is correct or not.

Parameters:
    datasets (str): list of allowed datasets
"""
def check_dataset(datasets: str):
    def wrapper(func):
        @functools.wraps(func)
        async def wrapped(msg: Sample):
            datasets_list = datasets.split(",")
            key = MessageKey(**json.loads(msg.key))
            if key.dataset in datasets_list:
                logger.debug("Dataset: OK")
                return await func(msg)
            else:
                logger.debug("Dataset: NO")
                return await func(None)
        return wrapped
    return wrapper

"""This annotation verifies if the state of the message is correct or not.

Parameters:
    states (str): list of allowed states
"""
def check_state(states: str):
    def wrapper(func):
        @functools.wraps(func)
        async def wrapped(msg: Sample):
            try:
                states_list = [Sample.States.Value(state) for state in states.split(",")]
                if msg.state in states_list:
                    logger.debug("State: OK")
                    return await func(msg)
                else:
                    logger.debug(f"State: NO, received {Sample.States.Name(msg.state)} requested {states}")
                    return await func(None)
                
            except ValueError:
                logger.error("State: NOT FOUND")
                return await func(None)
           
        return wrapped
    return wrapper

"""This annotation deserialize the incoming messages.

Parameters:
    serde (Protobuf class): the protocol buffer supporting class
"""
def parse_message(serde):
    def wrapper(func):
        @functools.wraps(func)
        async def wrapped(msg: ConsumerRecord):
            try:
                message = serde()
                message.ParseFromString(msg.value)
                logger.debug("Parsing: OK")
                return await func(message)
            except Exception:
                logger.debug("Dataset: ERROR")
                return await func(None)
        return wrapped
    return wrapper