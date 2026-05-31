from pydantic import BaseModel
class SearchParams(BaseModel):
    imgUrl: str  # 必须的字符串参数，表示图片路径
    topK: int = 10  # 可选的整数参数，默认值为10，表示返回最相近的图片数量
    usePairSimilarity: bool = True  # 可选的布尔参数，默认值为True，表示是否使用基于 tokens 的相似度计算
    callbackUrl: str  # 必须的字符串参数，表示回调 URL