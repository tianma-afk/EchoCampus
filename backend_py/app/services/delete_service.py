from loguru import logger
from core import milvus_service


def delete_vectors(uuids: list):
    """根据 uuid 列表从 Milvus 删除向量。"""
    if not uuids:
        logger.warning("delete_vectors 收到空列表，跳过")
        return
    expr = f"uuid in {uuids}"
    client = milvus_service.service.client
    result = client.delete(collection_name=milvus_service.COLLECTION_NAME, filter=expr)
    logger.info(f"Milvus 向量删除完成: count={len(uuids)}, result={result}")
