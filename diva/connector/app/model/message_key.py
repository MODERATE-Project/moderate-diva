from pydantic import BaseModel

class MessageKey(BaseModel):

    dataset: str
    timestamp: int