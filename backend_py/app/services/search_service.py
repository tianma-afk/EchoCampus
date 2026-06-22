import asyncio
import httpx
from asyncio import Semaphore
from loguru import logger

from utils.image_downloader import download_image_to_pil
from core.gpu_lock import gpu_lock
from core.dependencies import get_extractor
from core import milvus_service
from core.dense_features_manager import load_image_dense_features_async
from schemas.search import SearchParams

global_semaphore = Semaphore(10)


class SearchService:
    def __init__(self):
        pass

    async def search_process_with_limit(self, params: SearchParams):
        async with global_semaphore:
            await self.search_process(params)

    async def search_process(self, params: SearchParams):
        try:
            img = await download_image_to_pil(params.imgUrl)
            if img is None:
                logger.error(f"图片下载失败，任务取消: {params.imgUrl}")
                await self.search_callback(params.callbackUrl, "FAILED", [])
                return
            extractor = get_extractor()
            async with gpu_lock:  # 确保同一时间只有一个任务在使用 GPU
                global_desc, dense_features = await asyncio.to_thread(extractor.extract_complete_features, img)
            result = await milvus_service.service.search_similar_async(global_desc, params.topK)
            
            if params.usePairSimilarity:
                # 批量加载候选 dense_features
                candidate_ids = [hit["uuid"] for hit in result]
                raw_features = await asyncio.gather(*[load_image_dense_features_async(img_id) for img_id in candidate_ids])

                # 过滤掉没有缓存特征的候选（避免 torch.tensor(None) 崩溃）
                valid_pairs = [(img_id, feat) for img_id, feat in zip(candidate_ids, raw_features) if feat is not None]
                if valid_pairs:
                    valid_ids, valid_features = zip(*valid_pairs)
                    async with gpu_lock:  # 确保同一时间只有一个任务在使用 GPU
                        scores = await asyncio.to_thread(
                            extractor.pair_similarity_batch_single_query,
                            dense_features, list(valid_features)
                        )
                else:
                    valid_ids, scores = [], []

                id_to_score = dict(zip(valid_ids, scores if valid_pairs else []))
                results = []
                for hit in result:  # Milvus client 返回 [[hit1, hit2, ...]]
                    img_uuid = hit["uuid"]
                    item = {
                        "uuid": img_uuid,
                        "score": hit["score"],
                        "pair_similarity": id_to_score.get(img_uuid, None),
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
            logger.info(f"图像搜索任务成功: {params.callbackUrl}")            
            await self.search_callback(params.callbackUrl, "SUCCESS", results)

        except Exception as e:
            results = []
            logger.error("图像搜索任务失败: "+ str(e))
            await self.search_callback(params.callbackUrl, "FAILED", results)

    async def search_callback(self, callback_url, status, results=None):
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

            logger.error(f"回调在 5 次尝试后均失败: {callback_url}")
