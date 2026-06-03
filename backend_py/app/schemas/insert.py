from pydantic import BaseModel
from schemas.image import Image
from typing import List
class InsertParams(BaseModel):
    images: List[Image]
    callbackUrl: str