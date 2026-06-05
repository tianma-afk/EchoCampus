from fastapi import APIRouter
import asyncio

from schemas.insert import InsertParams
from services.vectorize_service import VectorizeService

router = APIRouter(prefix="/insert", tags=["管理端向量化图片"])


@router.post("")
async def insert_receive(params: InsertParams):
    '''
    插入图片接口
    输出json参数格式：
            {
                "images": [
                    { "uuid": "550e8400-e29b-41d4-a716-446655440000", "url": "http://minio:9000/image/imgs/uuid1/uuid2/uuid3/uuid4.jpg" },
                    { "uuid": "550e8400-e29b-41d4-a716-446655440001", "url": "http://minio:9000/image/imgs/uuid1/uuid2/uuid3/uuid5.jpg" }
                ],
                "callbackUrl": "http://localhost:8080/api/v1/internal/callback/vectorize/550e8400-e29b-41d4-a716-446655440000"
            }
    '''

    asyncio.create_task(VectorizeService().insert_process_with_limit(params))
    task_id = params.callbackUrl.split('/')[-1]
    return {
        "taskId": "alg-task-" + task_id
    }
