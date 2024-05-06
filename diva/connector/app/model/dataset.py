from typing import Optional

from pydantic import BaseModel


class Dataset(BaseModel):
    key: Optional[str]
    url: Optional[str]


class ObjectPendingQualityResponseItem(BaseModel):
    key: str
    asset_id: int
    id: int


class DownloadResponseItem(BaseModel):
    key: str
    download_url: str
