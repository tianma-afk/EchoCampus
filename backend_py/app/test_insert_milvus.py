# 单独运行该文件往milvus容器插入和postgre同步的测试数据：python test_insert_milvus.py
# 最后测试移动端识别全链路
import sys
from pathlib import Path
from io import BytesIO
import hashlib

import httpx
from PIL import Image
from loguru import logger

sys.path.insert(0, str(Path(__file__).resolve().parent))

import core.logger  # noqa: F401 — 初始化 loguru 配置
from core import milvus_service, token_manager
from services.pair_vpr import PairVPRExtractor

TEST_LANDMARKS = [
    {
        "name": "第4号图书馆",
        "category": "图书馆",
        "url": "https://ts1.tc.mm.bing.net/th/id/OIP-C._mRxMJjmcqtmlj-b3qMxCQHaDt?r=0&rs=1&pid=ImgDetMain&o=7&rm=3",
    },
    {
        "name": "第1号体育馆",
        "category": "体育场馆",
        "url": "https://ts1.tc.mm.bing.net/th/id/OIP-C.GLMTjgdLVNxbktVgOy6drgHaE8?r=0&rs=1&pid=ImgDetMain&o=7&rm=3",
    },
    {
        "name": "第3号饭堂",
        "category": "生活区",
        "url": "https://ts1.tc.mm.bing.net/th/id/R-C.3b917d34465986784410e9d483a7a713?rik=mTbDWR5bLW9eLw&riu=http%3a%2f%2f5b0988e595225.cdn.sohucs.com%2fimages%2f20190316%2f9cc554e181304452a19cec9665de1005.jpeg&ehk=jBq12tEp55SspXI3YWxiQ6IGNjBLkP4A3zxaErkifiA%3d&risl=&pid=ImgRaw&r=0",
    },
]


def url_to_uuid(url: str) -> str:
    digest = hashlib.md5(url.encode("utf-8")).hexdigest()
    return (
        digest[0:8]
        + "-"
        + digest[8:12]
        + "-"
        + digest[12:16]
        + "-"
        + digest[16:20]
        + "-"
        + digest[20:32]
    )


def main():
    logger.info("=" * 60)
    logger.info("测试地标图片录入容器 Milvus")
    logger.info("=" * 60)

    uuids = []
    for lm in TEST_LANDMARKS:
        uid = url_to_uuid(lm["url"])
        uuids.append(uid)
        logger.info(f"  {lm['name']} ({lm['category']})")
        logger.info(f"    UUID: {uid}")

    logger.info("-" * 60)
    logger.info("初始化 Milvus 容器连接...")
    milvus_service.milvus_init()

    logger.info("加载 Pair-VPR 模型 (vitB, fp16)...")
    extractor = PairVPRExtractor(model_type="vitB", use_fp16=True)

    vectors = []
    for lm, uid in zip(TEST_LANDMARKS, uuids):
        name = lm["name"]
        url = lm["url"]
        logger.info(f"处理: {name}")
        logger.info(f"  下载图片...")
        resp = httpx.get(url, timeout=30.0)
        resp.raise_for_status()
        img = Image.open(BytesIO(resp.content)).convert("RGB")
        logger.info(f"  尺寸: {img.size}")

        logger.info(f"  提取特征...")
        vector, tokens = extractor.extract_complete_features(img)
        logger.info(f"  向量维度: {len(vector)}")

        token_manager.save_image_tokens(uid, tokens)
        vectors.append(vector)
        logger.info(f"  Token 已保存")

    logger.info(f"批量写入容器 Milvus ({len(vectors)} 条)...")
    milvus_service.service.insert_vectors(vectors, uuids)

    logger.info("=" * 60)
    logger.info("录入完成！UUID 对照表：")
    logger.info("=" * 60)
    for lm, uid in zip(TEST_LANDMARKS, uuids):
        logger.info(f"  {lm['name']:10s}  {uid}")


if __name__ == "__main__":
    main()
