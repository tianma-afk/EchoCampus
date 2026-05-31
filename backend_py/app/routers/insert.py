from fastapi import APIRouter

from schemas.insert import InsertParams
from services.image_downloader import download_image_to_pil
from core.gpu_lock import gpu_lock
from core.dependencies import get_extractor
from core.token_manager import save_image_tokens_async
from core.milvus_lite import insert_vector_async, insert_vectors_async
import asyncio
import httpx

router = APIRouter(prefix="/insert", tags=["insert"])
@router.post("/")
async def insert_receive(params: InsertParams):
    asyncio.create_task(insert_process(params))
    task_id = params.callbackUrl.split('/')[-1]
    return {
        "taskId": "alg-task-" + task_id
        }

async def insert_process(params: InsertParams):
    img_queue = asyncio.Queue(maxsize=50)
    info_queue = asyncio.Queue(maxsize=50)

    download_task = asyncio.create_task(download_images(params, img_queue))
    process_task = asyncio.create_task(process_images(img_queue, info_queue))
    save_task = asyncio.create_task(save_images_info(info_queue))

    await asyncio.gather(download_task, process_task, save_task)
    await insert_recall(params)

async def insert_recall(params: InsertParams):
    async with httpx.AsyncClient() as client:
        data = {"result": "SUCCESS"}
        for attempt in range(5):
            try:
                response = await client.post(params.callbackUrl, json=data, timeout=5.0)
                if response.status_code == 200:
                    return  # 成功则退出
            except httpx.RequestError:
                pass  # 忽略异常，继续重试
            await asyncio.sleep(2 ** attempt)  # 指数退避
        print(f"回调在5次尝试后均失败: {params.callbackUrl}")


async def download_images(params: InsertParams, img_queue: asyncio.Queue):
    for image in params.images:
        image_uuid = image.uuid
        image_url = image.url
        img = await download_image_to_pil(image_url)
        await img_queue.put((img, image_uuid))
    await img_queue.put(None)  # 下载完成的标志

async def process_images(img_queue: asyncio.Queue,info_queue: asyncio.Queue):
    extractor = get_extractor()
    while True:
        img, image_uuid = await img_queue.get()
        if img is None:
            break
        async with gpu_lock:  # 确保同一时间只有一个任务在使用 GPU
            vector, token = await asyncio.to_thread(extractor.extract_complete_features, img)
        await info_queue.put((vector, token, image_uuid))
    await info_queue.put((None, None, None))  # 处理完成的标志

async def save_images_info(info_queue: asyncio.Queue):
    vectors = []
    uuids = []
    while True:
        vector, token, image_uuid = await info_queue.get()
        if vector is None and token is None and image_uuid is None:
            break
        await save_image_tokens_async(image_uuid, token)  # 异步保存 tokens
        vectors.append(vector)
        uuids.append(image_uuid)
        if len(vectors) >= 100:  # 每100个向量保存一次
            await insert_vectors_async(vectors, uuids)
            vectors = []
            uuids = []
    if len(vectors) > 0 and len(uuids) > 0:
        await insert_vectors_async(vectors, uuids)  # 保存剩余的向量
    