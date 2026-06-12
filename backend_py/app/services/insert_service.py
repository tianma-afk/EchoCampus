import asyncio
import httpx
from asyncio import Semaphore
from loguru import logger

from utils.image_downloader import download_image_to_pil
from core.gpu_lock import gpu_lock
from core.dependencies import get_extractor
from core.dense_features_manager import save_image_dense_features_async
from core import milvus_service
from schemas.insert import InsertParams

global_semaphore = Semaphore(10)


class InsertService:
    def __init__(self):
        pass

    async def insert_process_with_limit(self, params: InsertParams):
        async with global_semaphore:
            await self.insert_process(params)
    async def insert_process(self, params: InsertParams):
        img_queue = asyncio.Queue(maxsize=10)
        info_queue = asyncio.Queue(maxsize=10)

        download_task = asyncio.create_task(self.download_images(params, img_queue))
        process_task = asyncio.create_task(self.process_images(img_queue, info_queue))
        save_task = asyncio.create_task(self.save_images_info(info_queue))

        await asyncio.gather(download_task, process_task, save_task)
        await self.insert_recall(params)

    async def insert_recall(self, params: InsertParams):
        async with httpx.AsyncClient() as client:
            data = {"result": "SUCCESS"}
            for attempt in range(5):

                try:
                    response = await client.put(params.callbackUrl, json=data, timeout=5.0)
                    if response.status_code == 200:
                        return  # 成功则退出
                except httpx.TimeoutException:
                    continue    
                except httpx.RequestError:
                    pass  # 忽略异常，继续重试
                await asyncio.sleep(2 ** attempt)  # 指数退避
            logger.error(f"回调在 5 次尝试后均失败: {params.callbackUrl}")


    async def download_images(self, params: InsertParams, img_queue: asyncio.Queue):
        for image in params.images:
            image_uuid = image.uuid
            image_url = image.url
            img = await download_image_to_pil(image_url)
            await img_queue.put((img, image_uuid))
        await img_queue.put(None)  # 下载完成的标志

    async def process_images(self, img_queue: asyncio.Queue, info_queue: asyncio.Queue):
        extractor = get_extractor()
        while True:
            item = await img_queue.get()
            if item is None: # 检查队列是否为空
                break
            img, image_uuid = item
            async with gpu_lock:  # 确保同一时间只有一个任务在使用 GPU
                global_desc, dense_features = await asyncio.to_thread(extractor.extract_complete_features, img)
            await info_queue.put((global_desc, dense_features, image_uuid))
        await info_queue.put((None, None, None))  # 处理完成的标志

    async def save_images_info(self, info_queue: asyncio.Queue):
        global_descs = []
        uuids = []
        while True:
            global_desc, dense_features, image_uuid = await info_queue.get()
            if global_desc is None and dense_features is None and image_uuid is None:
                break
            await save_image_dense_features_async(image_uuid, dense_features)  # 异步保存 dense_features
            global_descs.append(global_desc)
            uuids.append(image_uuid)
            if len(global_descs) >= 100:  # 每100个向量保存一次
                await milvus_service.service.insert_vectors_async(global_descs, uuids)
                global_descs = []
                uuids = []
        if len(global_descs) > 0 and len(uuids) > 0:
            await milvus_service.service.insert_vectors_async(global_descs, uuids)  # 保存剩余的向量
        