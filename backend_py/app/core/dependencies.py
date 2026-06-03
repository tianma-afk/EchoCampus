from services.pair_vpr import PairVPRExtractor

_shared_extractor = None

def get_extractor(model_type = "vitB", use_fp16 = True):
    global _shared_extractor
    if _shared_extractor is None:
        _shared_extractor = PairVPRExtractor(model_type=model_type, use_fp16=use_fp16)
    return _shared_extractor