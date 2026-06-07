import uvicorn
from fastapi import FastAPI
from contextlib import asynccontextmanager
from loguru import logger

import core.logger  # noqa: F401 — 初始化 loguru 配置
from core.milvus_service import milvus_init
from core.dependencies import get_extractor
from routers import insert, search


@asynccontextmanager
async def lifespan(app: FastAPI):
    get_extractor()
    milvus_init()
    logger.info("服务已启动，资源已加载")

    yield

    logger.info("服务已关闭，资源已释放")


app = FastAPI(lifespan=lifespan)
app.include_router(insert.router)
app.include_router(search.router)


@app.get("/")
def root():
    return {"message": "图搜图后端根路由，访问 /search 或 /insert 来使用搜索或插入功能"}


if __name__ == "__main__":
    uvicorn.run(
        "main:app", host="127.0.0.1", port=8000, reload=False
    )
