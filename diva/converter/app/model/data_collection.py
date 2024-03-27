from pydantic import BaseModel
from typing import Optional

class DataCollectionModel(BaseModel):
    
    timestamp: Optional[str]
    sourceType: Optional[str]
    sourceID: Optional[str]
    infoType: Optional[str]
    dataType: Optional[str]
    dataItemID: Optional[str]
    metricTypeID: Optional[str]
    metricValue: Optional[dict]
    measureUnit: Optional[str]
    