from fastapi import FastAPI
import uvicorn
from pydantic import BaseModel # 接收时：它能一键把前端传来的 JSON 字典，直接变成你代码里可以点来点去的 Python 对象

import core.milvus_lite 
from contextlib import asynccontextmanager

from core.dependencies import get_extractor
from routers import insert, search

@asynccontextmanager
async def lifespan(app: FastAPI):
    get_extractor()
    core.milvus_lite.init() # 初始化 Milvus 服务单例
    print("服务已启动，资源已加载")

    yield
    
    print("服务已关闭，资源已释放")

app = FastAPI(lifespan=lifespan)
app.include_router(insert.router)
app.include_router(search.router)

@app.get("/")
def root():
    return {"message": "图搜图后端根路由，访问 /search 或 /insert 来使用搜索或插入功能"}



if __name__ == "__main__":
    uvicorn.run(
        "main:app", host="127.0.0.1", port=8000, reload=False
    )  # 设置运行参数：网络地址 端口号 是否开启热更新
    
# uvicorn main:app --reload  开启热更新

# http://127.0.0.1:8000/docs
