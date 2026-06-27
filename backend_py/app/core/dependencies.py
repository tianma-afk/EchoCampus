from services.pair_vpr import PairVPRExtractor
from core.settings import settings

_shared_extractor = None

def get_extractor(model_type: str | None = None, use_fp16: bool = True):
    """获取模型单例。model_type 不传则读取 .env 中的 MODEL_TYPE 配置。"""
    if model_type is None:
        model_type = settings.MODEL_TYPE
    global _shared_extractor
    if _shared_extractor is None:
        _shared_extractor = PairVPRExtractor(model_type=model_type, use_fp16=use_fp16)
    return _shared_extractor