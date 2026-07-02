from fastapi import APIRouter
from loguru import logger

from schemas.delete import DeleteParams
from services.delete_service import delete_vectors

router = APIRouter(prefix="/delete", tags=["管理端删除 Milvus 向量"])


@router.post("")
async def delete_receive(params: DeleteParams):
    '''
    删除 Milvus 向量接口
    参数格式：
    {
        "uuids": ["550e8400-e29b-41d4-a716-446655440000", "550e8400-e29b-41d4-a716-446655440001"]
    }
    '''
    logger.info(f"收到 Milvus 向量删除请求: count={len(params.uuids)}")
    delete_vectors(params.uuids)
    return {"result": "SUCCESS"}
