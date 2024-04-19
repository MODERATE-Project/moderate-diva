import asyncio

from services import manager, inbound_connector, cron

async def main():
    await asyncio.gather(
        manager.start(),
        inbound_connector.start(),
        cron.start()
    )
    
if __name__ == "__main__":
    loop = asyncio.get_event_loop()
    
    try:
        asyncio.run(main())
    except KeyboardInterrupt:
        loop.close()