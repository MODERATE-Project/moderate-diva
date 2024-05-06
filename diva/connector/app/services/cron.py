import asyncio
import json
from typing import List

import aiohttp
from aiologger import Logger
from aiologger.formatters.base import Formatter
from model.dataset import (
    Dataset,
    DownloadResponseItem,
    ObjectPendingQualityResponseItem,
)
from utils.queues import cron_queue
from utils.settings import settings

formatter = Formatter("[%(asctime)s][%(module)s][%(levelname)s]: %(message)s")
logger = Logger.with_default_handlers(formatter=formatter)


async def get_token() -> str:
    """
    Retrieves a JWT token by sending a POST request to the API.

    Returns:
        str: The access token retrieved from the server.
        None: If there was an error during the request.
    """

    data = {"username": settings.auth_user, "password": settings.auth_password}

    async with aiohttp.ClientSession() as session:
        async with session.post(settings.auth_url, data=data) as response:
            response.raise_for_status()
            resp_data = json.loads(await response.text())
            return resp_data["access_token"]


async def get_asset_objects_pending_quality(
    access_token,
) -> List[ObjectPendingQualityResponseItem]:
    """
    Retrieves a list of asset objects pending quality check.

    Args:
        access_token (str): The access token for authentication.

    Returns:
        list: A list of asset objects pending quality check, or None if an error occurs.
    """

    headers = {"Authorization": f"Bearer {access_token}"}

    async with aiohttp.ClientSession() as session:
        async with session.get(
            f"{settings.asset_url}/object/quality-check", headers=headers
        ) as response:
            response.raise_for_status()
            logger.info(f"Assets List Success")
            resp_data = await response.json()
            return [ObjectPendingQualityResponseItem(**obj) for obj in resp_data]


async def get_download_urls(access_token, asset_id) -> List[DownloadResponseItem]:
    """
    Retrieves the download URLs for a given asset ID using the provided access token.

    Args:
        access_token (str): The access token used for authentication.
        asset_id (str): The ID of the asset for which to retrieve the download URLs.

    Returns:
        dict or None: A dictionary containing the download URLs if the request is successful,
        otherwise None.
    """

    headers = {"Authorization": f"Bearer {access_token}"}

    async with aiohttp.ClientSession() as session:
        async with session.get(
            f"{settings.asset_url}/{asset_id}/download-urls", headers=headers
        ) as response:
            response.raise_for_status()
            logger.info(f"Download URLs for Asset {asset_id} Success")
            resp_data = await response.json()
            return [DownloadResponseItem(**obj) for obj in resp_data]


async def ack_asset_objects(access_token, asset_object_ids):
    """
    Acknowledge asset objects by sending a POST request to the asset URL.

    Args:
        access_token (str): The access token for authorization.
        asset_object_ids (list): A list of asset object IDs to acknowledge.

    Returns:
        None
    """

    headers = {"Authorization": f"Bearer {access_token}"}

    request_json_body = {
        "asset_object_id": asset_object_ids,
        "pending_quality_check": False,
    }

    async with aiohttp.ClientSession() as session:
        async with session.post(
            f"{settings.asset_url}/object/quality-check",
            headers=headers,
            json=request_json_body,
        ) as response:
            response.raise_for_status()
            logger.info(f"Acknowledged asset objects: {asset_object_ids}")


async def get_pending_datasets():
    """
    Retrieves pending datasets and their download URLs.

    Returns:
        A list of Dataset objects, each containing the asset object key and download URL.
    """

    token = await get_token()
    pending_asset_objects = await get_asset_objects_pending_quality(access_token=token)
    asset_object_keys = [item.key for item in pending_asset_objects]
    asset_ids = set([item.asset_id for item in pending_asset_objects])
    urls_dict = {}
    asset_object_ids_ack = []

    for asset_id in asset_ids:
        download_urls = await get_download_urls(access_token=token, asset_id=asset_id)

        download_urls = [
            item for item in download_urls if item.key in asset_object_keys
        ]

        for url_item in download_urls:
            urls_dict[url_item.key] = url_item.download_url

            asset_object_id = next(
                pending_item.id
                for pending_item in pending_asset_objects
                if pending_item.key == url_item.key
            )

            asset_object_ids_ack.append(asset_object_id)

    logger.debug(f"Asset objects download URLs: {urls_dict}")

    if len(asset_object_ids_ack) > 0:
        await ack_asset_objects(
            access_token=token, asset_object_ids=asset_object_ids_ack
        )

    return [Dataset(key=key, url=url) for key, url in urls_dict.items()]


async def cron():
    while True:
        try:
            datasets = await get_pending_datasets()
            logger.info(f"Putting datasets in queue: {datasets}")

            for dataset in datasets:
                await cron_queue.put(dataset)
        except Exception:
            logger.error("Unexpected error: Skipping poll iteration", exc_info=True)

        await asyncio.sleep(settings.polling_time)


async def start():
    await cron()
