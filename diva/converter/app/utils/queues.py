import asyncio

# Queue of messages sent to the inbound converter service
inbound_queue = asyncio.Queue()
# Queue of messages sent to the outbound converter service
outbound_queue = asyncio.Queue()

# Queue of messages published by the connector and in need of conversion
sample_queue = asyncio.Queue()
# Queue of messages read by the sample topic that need to be exported
connector_queue = asyncio.Queue()