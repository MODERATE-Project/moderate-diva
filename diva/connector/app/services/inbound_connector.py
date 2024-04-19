import asyncio
import time
import json

import serde.sample_pb2 as s
from model.dataset import Dataset
from utils.queues import cron_queue, connector_queue

import pandas as pd

from aiologger import Logger
from aiologger.formatters.base import Formatter
formatter = Formatter('[%(asctime)s][%(module)s][%(levelname)s]: %(message)s')
logger = Logger.with_default_handlers(formatter=formatter)

"""Converts the outgoing sample into JSON format.
"""     
async def convert_in():
    while True:
        dataset: Dataset = await cron_queue.get()     

        try:
            df = pd.read_parquet(dataset.url, engine='pyarrow')
               
            for i, d in df.iterrows():

                string_data = {
                    "topic": s.StringArray(element=[dataset.key]),
                    "JSON": s.StringArray(element=[json.dumps(d.to_dict())])
                }
                
                sample = s.Sample(
                    ts=time.time_ns()//1000,
                    state=s.Sample.States.NEW,
                    dataset=dataset.key,
                    string_data=string_data
                )

                logger.info(f"Published new sample for {dataset.key}")
                await connector_queue.put(sample)
                
        except:
            logger.error(f"Invalid Dataset with key {dataset.key}")
             
async def start():
    await convert_in()