import asyncio
import time

import serde.sample_pb2 as s
from utils.queues import outbound_queue, connector_queue
from model.data_collection import DataCollectionModel

"""Creates an entry or nested map in the JSON object.

Parameters:
    data (dict): The JSON object to populate.
    key (str):   The key for the entry or map.
    value (str): The value to insert.
"""
def create_entry_or_map(data, key, value):
    keys = key.split("/")
    if len(keys) > 1:  
        inner_data = {}
        create_entry_or_map(inner_data, keys[1], value)
        data[keys[0]] = inner_data
    else:
        data[key] = value
       
"""Converts the outgoing sample into JSON format.
"""     
async def convert_out():
    while True:
        item = await outbound_queue.get()     
        data = {}
        
        for data_map in [item.float_data, item.string_data, item.bool_data]:
            for k, v in data_map.items():
                create_entry_or_map(data, k, v.element[0])
        
        info_type = f"{s.Sample.States.Name(item.state).lower()}Data"
        model = DataCollectionModel(
            timestamp=str(time.time_ns()//1000),
            sourceID=item.metadata["sourceID"],
            sourceType=item.metadata["sourceType"],
            dataItemID=item.metadata["dataItemID"],
            dataType=item.metadata["dataType"],
            infoType=info_type,
            measureUnit="Single",
            metricTypeID=item.key,
            metricValue=data
        )

        topic = f"{model.sourceType}/{model.sourceID}/{info_type}/{model.dataType}/{model.dataItemID[1:] if model.dataItemID.startswith('/') else model.dataItemID}/"
        string_data = {
            "topic": s.StringArray(element=[topic]),
            "JSON": s.StringArray(element=[model.json()])
        }
        
        sample = s.Sample(
            ts=item.ts,
            state=item.state,
            dataset=item.dataset,
            string_data=string_data
        )

        await connector_queue.put(sample)
             
async def start():
    await convert_out()