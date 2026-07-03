import asyncio
import time
import httpx
import numpy as np
from asyncio import Semaphore
from loguru import logger

from utils.image_downloader import download_image_to_pil
from core.gpu_lock import gpu_lock
from core.dependencies import get_extractor
from core import milvus_service
from core.dense_features_manager import load_image_dense_features_http_async
from schemas.search import SearchParams

global_semaphore = Semaphore(10)


class SearchService:
    def __init__(self):
        pass

    async def search_process_with_limit(self, params: SearchParams):
        async with global_semaphore:
            await self.search_process(params)

    async def search_process(self, params: SearchParams):
        t0 = time.time()
        try:
            # 前置检查：Milvus 图像库是否为空
            collection_count = await milvus_service.service.get_collection_count_async()
            if collection_count == 0:
                logger.warning("Milvus 图像库为空，取消搜索任务")
                await self.search_callback(params.callbackUrl, "LIBRARY_EMPTY", [])
                return

            img = await download_image_to_pil(params.imgUrl)
            t1 = time.time()
            logger.info(f"[计时] 下载图片: {t1-t0:.2f}s")
            if img is None:
                logger.error(f"图片下载失败，任务取消: {params.imgUrl}")
                await self.search_callback(params.callbackUrl, "FAILED", [])
                return
            extractor = get_extractor()
            async with gpu_lock:
                global_desc, dense_features = await asyncio.to_thread(extractor.extract_complete_features, img)
            t2 = time.time()
            logger.info(f"[计时] 特征提取: {t2-t1:.2f}s  (global_desc len={len(global_desc) if global_desc else 0}, dense_features shape={np.array(dense_features).shape if dense_features is not None else None})")
            result = await milvus_service.service.search_similar_async(global_desc, params.topK)
            t3 = time.time()
            logger.info(f"[计时] Milvus 搜索 (topK={params.topK}): {t3-t2:.2f}s  命中 {len(result)} 条")

            if params.usePairSimilarity:
                candidate_ids = [hit["uuid"] for hit in result]
                raw_features = await asyncio.gather(*[load_image_dense_features_http_async(img_id) for img_id in candidate_ids])
                t4 = time.time()
                logger.info(f"[计时] 加载 dense_features ({len(candidate_ids)}个): {t4-t3:.2f}s  有效: {sum(1 for f in raw_features if f is not None)}/{len(raw_features)}")

                valid_pairs = [(img_id, feat) for img_id, feat in zip(candidate_ids, raw_features) if feat is not None]
                if valid_pairs:
                    valid_ids, valid_features = zip(*valid_pairs)
                    async with gpu_lock:
                        scores = await asyncio.to_thread(
                            extractor.pair_similarity_batch_single_query,
                            dense_features, list(valid_features)
                        )
                    t5 = time.time()
                    logger.info(f"[计时] Pair similarity: {t5-t4:.2f}s  ({len(valid_ids)} 个候选)")
                else:
                    valid_ids, scores = [], []
                    t5 = time.time()

                id_to_score = dict(zip(valid_ids, scores if valid_pairs else []))
                results = []
                for hit in result:
                    img_uuid = hit["uuid"]
                    item = {
                        "uuid": img_uuid,
                        "score": hit["score"],
                        "pair_similarity": id_to_score.get(img_uuid, None),
                    }
                    results.append(item)
            else:
                results = []
                for hit in result:
                    img_uuid = hit["uuid"]
                    item = {
                        "uuid": img_uuid,
                        "score": hit["score"],
                        "pair_similarity": "None",
                    }
                    results.append(item)

            t6 = time.time()
            logger.info(f"[计时] 回调发送前 总计: {t6-t0:.2f}s  结果数={len(results)}")
            await self.search_callback(params.callbackUrl, "SUCCESS", results)
            t7 = time.time()
            logger.info(f"[计时] 回调完成: {t7-t6:.2f}s")

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
