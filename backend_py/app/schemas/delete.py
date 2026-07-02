from pydantic import BaseModel
from typing import List


class DeleteParams(BaseModel):
    uuids: List[str]
