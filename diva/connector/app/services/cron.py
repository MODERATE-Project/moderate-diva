import asyncio
import aiohttp

from utils.queues import cron_queue
from utils.settings import settings

from serde import sample_pb2

from model.dataset import Dataset

import json

from aiologger import Logger
from aiologger.formatters.base import Formatter
formatter = Formatter('[%(asctime)s][%(module)s][%(levelname)s]: %(message)s')
logger = Logger.with_default_handlers(formatter=formatter)


"""Gets the authentication JWT token

Returns:
    token (str): the JWT just requested
"""
async def get_token():
    data = {
        'username': settings.auth_user,
        'password': settings.auth_password
    }

    async with aiohttp.ClientSession() as session:
        async with session.post(settings.auth_url, data=data) as response:
            if response.status == 200:
                logger.info(f"JWT Token Success")
                text_response = await response.text()
                return json.loads(text_response)['access_token']
            else:
                logger.error(f"Error: {response.status} - {await response.text()}")
                return None

"""Gets the list of assets

Parameters:
    access_token (str): the JWT token for authenticating the request
"""
async def get_assets(access_token):
    headers = {
        'Authorization': f'Bearer {access_token}'
    }

    async with aiohttp.ClientSession() as session:
        async with session.get(settings.asset_url, headers=headers) as response:
            if response.status == 200:
                logger.info(f"Assets List Success")
                return await response.json()
            else:
                logger.error(f"Error: {response.status} - {await response.text()}")
                return None

"""Gets the URL of each asset object

Parameters:
    access_token (str): the JWT token for authenticating the request
    asset_id (int): the asset for which it is requested to get the download URLs
"""
async def get_urls(access_token, asset_id):
    headers = {
        'Authorization': f'Bearer {access_token}'
    }

    async with aiohttp.ClientSession() as session:
        async with session.get(f"{settings.asset_url}/{asset_id}/download-urls", headers=headers) as response:
            if response.status == 200:
                logger.info(f"Download URLs for Asset {asset_id} Success")
                return await response.json()
            else:
                logger.error(f"Error: {response.status} - {await response.text()}")
                return None
            
"""Executes the assets pooling
"""
async def cron():
    past_keys = []

    while True:
        token = await get_token()
        if token is not None:
            assets = await get_assets(token)

            for asset in assets:
                asset_objects = await get_urls(token, asset["id"])
                for obj in asset_objects:    
                    
                    if obj["key"] not in past_keys:
                        dataset = Dataset(
                            key=obj["key"],
                            url=obj["download_url"]
                        )

                        past_keys.append(obj["key"])
                        
                        logger.info(f"Sent new asset url: {dataset}")
                        await cron_queue.put(dataset)
        
        await asyncio.sleep(settings.polling_time)

async def start():
    await cron()