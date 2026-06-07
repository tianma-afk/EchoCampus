from pymilvus import MilvusClient, CollectionSchema, FieldSchema, DataType
from typing import Optional
import asyncio
from loguru import logger

from . import settings

MILVUS_URI = settings.settings.MILVUS_URI

COLLECTION_NAME = "image_collection"
VECTOR_DIM = 512
METRIC_TYPE = "IP"

INDEX_PARAMS = {
    "metric_type": METRIC_TYPE,
    "index_type": "HNSW",
    "params": {"M": 32, "efConstruction": 200},
}

SEARCH_PARAMS = {
    "metric_type": METRIC_TYPE,
    "params": {"ef": 128},
}


class MilvusService:
    """Milvus 向量数据库服务"""

    def __init__(self, uri: str):
        self.client = MilvusClient(uri=uri)
        self._setup_collection()
        self._setup_index()

    def _setup_collection(self):
        if self.client.has_collection(COLLECTION_NAME):
            logger.info(f"集合 {COLLECTION_NAME} 已存在")
            return
        schema = CollectionSchema([
            FieldSchema(name="uuid", dtype=DataType.VARCHAR, max_length=36, is_primary=True),
            FieldSchema(name="vector", dtype=DataType.FLOAT_VECTOR, dim=VECTOR_DIM),
        ])
        self.client.create_collection(collection_name=COLLECTION_NAME, schema=schema)
        logger.info(f"集合 {COLLECTION_NAME} 创建成功")

    def _setup_index(self):
        indexes = self.client.list_indexes(collection_name=COLLECTION_NAME)
        if "vector" in indexes:
            logger.info("索引已存在，跳过创建")
            return
        index_params = self.client.prepare_index_params()
        index_params.add_index(
            field_name="vector",
            index_type=INDEX_PARAMS["index_type"],
            metric_type=INDEX_PARAMS["metric_type"],
            params=INDEX_PARAMS["params"],
        )
        self.client.create_index(collection_name=COLLECTION_NAME, index_params=index_params)
        logger.info("索引创建成功")

    # ========== 同步 ==========

    def load_collection(self):
        self.client.load_collection(collection_name=COLLECTION_NAME)

    def insert_vector(self, vector, uuid: str):
        if len(uuid) != 36:
            logger.warning(f"uuid '{uuid}' 长度不是36位")
        data = [{"uuid": uuid, "vector": vector}]
        return self.client.insert(collection_name=COLLECTION_NAME, data=data)

    def insert_vectors(self, vectors, uuids: list):
        if not vectors or not uuids or len(vectors) != len(uuids):
            logger.error("vectors/uuids 不能为空且长度必须一致")
            return None
        data = [{"uuid": uuids[i], "vector": vectors[i]} for i in range(len(vectors))]
        return self.client.insert(collection_name=COLLECTION_NAME, data=data)

    def search_similar(self, query_vector, top_k: int = 10):
        results = self.client.search(
            collection_name=COLLECTION_NAME,
            data=[query_vector],
            limit=top_k,
            search_params=SEARCH_PARAMS,
            output_fields=["uuid"],
        )
        return [{"uuid": r["uuid"], "score": r["distance"]} for r in results[0]]

    # ========== 异步 ==========

    async def insert_vector_async(self, vector, uuid: str):
        if len(uuid) != 36:
            logger.warning(f"uuid '{uuid}' 长度不是36位")
        data = [{"uuid": uuid, "vector": vector}]
        return await asyncio.to_thread(self.client.insert, collection_name=COLLECTION_NAME, data=data)

    async def insert_vectors_async(self, vectors, uuids: list):
        if not vectors or not uuids or len(vectors) != len(uuids):
            logger.error("vectors/uuids 不能为空且长度必须一致")
            return None
        data = [{"uuid": uuids[i], "vector": vectors[i]} for i in range(len(vectors))]
        return await asyncio.to_thread(self.client.insert, collection_name=COLLECTION_NAME, data=data)

    async def load_collection_async(self):
        await asyncio.to_thread(self.client.load_collection, collection_name=COLLECTION_NAME)

    async def search_similar_async(self, query_vector, top_k: int = 10):
        results = await asyncio.to_thread(
            self.client.search,
            collection_name=COLLECTION_NAME,
            data=[query_vector],
            limit=top_k,
            search_params=SEARCH_PARAMS,
            output_fields=["uuid"],
        )
        return [{"uuid": r["uuid"], "score": r["distance"]} for r in results[0]]


# ========== 模块级单例 ==========

service: Optional[MilvusService] = None


def milvus_init():
    """启动时调用，初始化 MilvusService 单例。"""
    global service
    if service is not None:
        return
    service = MilvusService(MILVUS_URI)
    logger.info(f"Milvus 连接成功 ({MILVUS_URI})")
    service.load_collection()
    logger.info("集合已加载到内存")
