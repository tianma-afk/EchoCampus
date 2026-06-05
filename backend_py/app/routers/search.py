from fastapi import APIRouter

import asyncio
from services.image_downloader import download_image_to_pil
from core.gpu_lock import gpu_lock
from schemas.search import SearchParams
from core.dependencies import get_extractor
from core import milvus_lite
from core.token_manager import load_image_tokens_async
import httpx
from asyncio import Semaphore

router = APIRouter(prefix="/search", tags=["search"])
global_semaphore = Semaphore(10)


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
    asyncio.create_task(search_process_with_limit(params))
    return {"taskId": "alg-task-" + task_id}

async def search_process_with_limit(params: SearchParams):
    async with global_semaphore:
        await search_process(params)
async def search_process(params: SearchParams):
    try:
        img = await download_image_to_pil(params.imgUrl)
        extractor = get_extractor()
        async with gpu_lock:  # 确保同一时间只有一个任务在使用 GPU
            vector, token = await asyncio.to_thread(extractor.extract_complete_features, img)
        result = await milvus_lite.service.search_similar_async(vector, params.topK)
        
        if params.usePairSimilarity:
            # 批量加载候选 tokens
            candidate_ids = [hit["uuid"] for hit in result]
            candidate_tokens = await asyncio.gather(*[load_image_tokens_async(img_id) for img_id in candidate_ids]) 

            # 批量计算相似度（优化版）
            async with gpu_lock:  # 确保同一时间只有一个任务在使用 GPU
                scores = await asyncio.to_thread(extractor.pair_similarity_batch_single_query, token, candidate_tokens)

            results = []
            for hit, score in zip(result, scores):  # Milvus client 返回 [[hit1, hit2, ...]]
                img_uuid = hit["uuid"]
                item = {
                    "uuid": img_uuid,
                    "score": hit["score"],
                    "pair_similarity": score,
                }
                results.append(item)
        else:
            results = []
            for hit in result:  # Milvus client 返回 [[hit1, hit2, ...]]
                img_uuid = hit["uuid"]
                item = {
                    "uuid": img_uuid,
                    "score": hit["score"],
                    "pair_similarity": "None",
                }
                results.append(item)
                    
        await search_callback(params.callbackUrl, "SUCCESS", results)

    except Exception as e:
        results = []
        await search_callback(params.callbackUrl, "FAILED", results)

async def search_callback(callback_url, status, results=None):
    async with httpx.AsyncClient() as client:
        for attempt in range(5):
            try:
                response = await client.put(
                    url=callback_url,
                    json={"result": status, "matches": results},
                    timeout=5.0,
                )  
                if response.status_code == 200:
                    return  # 成功则退出
            except httpx.TimeoutException:
                continue
            except httpx.RequestError:
                pass  # 忽略异常，继续重试
            await asyncio.sleep(2**attempt)  # 指数退避

        print(f"回调在5次尝试后均失败: {callback_url}")
