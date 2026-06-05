from fastapi import APIRouter
import asyncio

from schemas.search import SearchParams
from services.search_service import SearchService

router = APIRouter(prefix="/search", tags=["search"])


@router.post("")
async def search_receive(params: SearchParams):
    '''
      搜索接口
      输入json参数格式：
        {
            "imgUrl": "http://localhost:9000/campus/images/2026/05/31/5f410d56.jpg",
            "callbackUrl": "http://localhost:8080/api/v1/internal/callback/search/550e8400-e29b-41d4-a716-446655440000"
            "topK": 10,
            "usePairSimilarity": true,
        }
    '''
    task_id = params.callbackUrl.split("/")[-1]
    asyncio.create_task(SearchService().search_process_with_limit(params))
    return {"taskId": "alg-task-" + task_id}
