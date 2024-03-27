import asyncio

from utils.queues import inbound_queue, sample_queue
from serde import sample_pb2

from model.data_collection import DataCollectionModel

import json

"""Parses and adds a float value to the common map.

Parameters:
    field (int):  The field value to parse
    header (str): The header associated with the field.
    map (dict):   The map to add the parsed float value.

Returns:
    bool: true if the parsing and addition are successful, false otherwise.
"""
def parse_float(field, header, map):
    success = False
    try:
        value = float(field)

        if header in map:
            map[header].append(value)
        else:
            map[header] = [value]

        success = True
    except ValueError:
        success = False

    return success

"""Parses and adds a boolean value to the map.

Parameters:
    field (int):  The field value to parse
    header (str): The header associated with the field.
    map (dict):   The map to add the parsed float value.

Returns:
    bool: true if the parsing and addition are successful, false otherwise.
"""
def parse_boolean(field, header, map):
    success = False
    value = None
    if field:
        if field.lower() == 'true':
            value = True
        elif field.lower() == 'false':
            value = False

    if value is not None:
        if header in map:
            map[header].append(value)
        else:
            map[header] = [value]

        success = True

    return success

"""Parses the metric value and constructs a sample.

Parameters:
    metricValue (dict): The metric value to parse.
    prefixItem (str):   The prefix item for nested values.
    dataset (str):      The dataset associated with the sample.
    key (str):          The key associated with the sample.

Returns:
    Sample: The parsed sample.
""" 
def parse_metric_value(metric_value, prefix_item, dataset, key):
        float_values = {}
        bool_values = {}
        string_values = {}

        for item_key, item_value in metric_value.items():
            item_key = f"{prefix_item}/{item_key}" if prefix_item else item_key

            if isinstance(item_value, dict):
                nested = parse_metric_value(item_value, item_key, dataset, key)

                for k, v in nested['float_data'].items():
                    float_values[k].append(v)

                for k, v in nested['bool_data'].items():
                    bool_values[k].append(v)

                for k, v in nested['string_data'].items():
                    string_values[k].append(v)
            else:
                if item_value is not None:
                    item_value = str(item_value)
                    parsed = False
                    parsed = parse_float(item_value, item_key, float_values)

                    if not parsed:
                        parsed = parse_boolean(item_value, item_key, bool_values)

                    if not parsed:
                        if item_key in string_values:
                            string_values[item_key].append(item_value)
                        else:
                            string_values[item_key] = [item_value]

        
        for k, v in float_values.items():
            float_values[k] = sample_pb2.FloatArray(element=v)
        for k, v in string_values.items():
            string_values[k] = sample_pb2.StringArray(element=v)
        for k, v in bool_values.items():
            bool_values[k] = sample_pb2.BoolArray(element=v)
            
        sample = sample_pb2.Sample(
            float_data=float_values,
            string_data=string_values,
            bool_data=bool_values
        )
        
        sample.state = sample_pb2.Sample.States.NEW
        sample.dataset = dataset
        sample.key = key

        return sample

    /**
     * Converts the incoming sample data into samples.
     *
     * @param sample The incoming sample data to convert.
     * @return A Flux emitting the converted samples.
     * @throws MQTTMessageNotDeserializable if the incoming MQTT message cannot be deserialized.
    */

"""Converts the incoming sample data into samples.
"""
async def convert_in():
    while True:
        item: sample_pb2.Sample = await inbound_queue.get()
        message: sample_pb2.Sample.StringArray = item.string_data["JSON"].element[0]
        model = DataCollectionModel(**json.loads(message))
        dataset = model.dataItemID
        results = []
        
        if model.measureUnit:
            if model.measureUnit == 'List':
                for element in model.metricValue:    
                    results.append(parse_metric_value(element, '', dataset, model.metricTypeID))

            if model.measureUnit == 'Map':
                results.append(parse_metric_value(model.metricValue, '', dataset, model.metricTypeID))

        else:
            results.append(parse_metric_value(element, '', dataset, model.metricTypeID))

         
        metadata = {
            "sourceID": model.sourceID,
            "sourceType": model.sourceType,
            "metricTypeID": model.metricTypeID,
            "infoType": model.infoType,
            "dataType": model.dataType,
            "dataItemID": model.dataItemID     
        }            
        
        for result in results:
            for k, v in metadata.items():
                result.metadata[k] = v
            
            await sample_queue.put(result)
        
async def start():
    await convert_in()