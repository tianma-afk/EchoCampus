from pydantic import BaseModel
class Image(BaseModel):
    uuid: str  # 36位
    url: str