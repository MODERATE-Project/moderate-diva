import asyncio

# Queue of messages sent to the inbound converter service
inbound_queue = asyncio.Queue()
# Queue of messages sent to the cron service
cron_queue = asyncio.Queue()

# Queue of messages that need to be published inside the DIVA
connector_queue = asyncio.Queue()