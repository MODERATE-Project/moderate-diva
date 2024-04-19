from pydantic import BaseModel
from typing import Optional

class Dataset(BaseModel):
    
    key: Optional[str]
    url: Optional[str]