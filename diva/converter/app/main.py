import asyncio

from services import manager, inbound_converter, outbound_converter

async def main():
    await asyncio.gather(
        manager.start(), 
        inbound_converter.start(),
        outbound_converter.start()
    )
    
if __name__ == "__main__":
    loop = asyncio.get_event_loop()
    
    try:
        asyncio.run(main())
    except KeyboardInterrupt:
        loop.close()