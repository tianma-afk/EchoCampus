from fastapi import FastAPI
import uvicorn
from pydantic import BaseModel # 接收时：它能一键把前端传来的 JSON 字典，直接变成你代码里可以点来点去的 Python 对象

import core.milvus_lite
import core.token_manager
from services.pair_vpr import PairVPRExtractor
import os
from pathlib import Path
import hashlib
from typing import List, Optional
import asyncio
import Minio

app = FastAPI()
extractor = PairVPRExtractor(model_type="vitB", use_fp16=True)
core.milvus_lite.load_collection()

@app.get("/")
def root():
    return {"message": "图搜图后端接口已启动！"}

class Picture(BaseModel):
    uuid: str = None  # 36位
    pic_url: str  # 现在为路径
    
class InsertParams(BaseModel):
    Pictures: List[Picture]
    usePairVPR: bool
    recallurl: str

@app.post("/insert")
async def insert_receive(params: InsertParams):
    asyncio.create_task(insert_process(params))
    return {"result": "SUCCESS"}

async def insert_process(params: InsertParams):


# 1. 专门定义一个类，规定好要传哪两个数据
class SearchParams(BaseModel):
    pic_path: str  # 必须的字符串参数，表示图片路径
    top_k: int = 10  # 可选的整数参数，默认值为10


@app.post("/search")
def search(params: SearchParams):
    '''
    搜索接口
    输入json参数格式：
    {
        "pic_path": "图片路径", #后将改为url
        "top_k": 10 #返回最相近的图片数量
    }
    
    '''
    # 提取查询图片的完整特征（包括 tokens）
    goal_vector, goal_token = extractor.extract_complete_features(params.pic_path)

    result = core.milvus_lite.search_similar(goal_vector, params.top_k)
    # print("搜索结果:")
    results_with_scores = []
    json_results_1 = []
    for hit in result:  # Milvus client 返回 [[hit1, hit2, ...]]
        img_uuid = hit["uuid"]
        filename = hit["filename"] 
        item = {"filename": filename, "uuid": img_uuid, "score": hit["score"]}
        json_results_1.append(item)
        score = extractor.pair_similarity_from_cached_tokens(
            goal_token, core.token_manager.load_image_tokens(img_uuid)
        )
        results_with_scores.append((hit, score))

    # 按 score 降序排序
    results_with_scores.sort(key=lambda x: x[1], reverse=True)

    # 打印排序后的结果
    json_results_2 = []
    # print("搜索结果（按相似度排序）:")
    for hit, score in results_with_scores:
        img_uuid = hit["uuid"]
        filename = hit["filename"]
        item = {"filename": filename, "uuid": img_uuid, "score": score}
        json_results_2.append(item)
    return {
        "message": "图搜图成功",
        "results_1": json_results_1,  # 初始比较的结果
        "results_2": json_results_2,  # 筛选后的结果
    }
  
# @app.post("/insert_one")
# def add(params: InsertParam):
#     '''
#     添加一个图片
#     输入json格式：
#     {
#         "uuid": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
#         "pic_url": "xxx
#     }
#     '''
    
#     f = os.path.basename(params.pic_url)# 获取文件名
#     if f.lower().endswith((".png", ".jpg", ".jpeg")):
#         stable_uuid = hashlib.md5(f.encode("utf-8")).hexdigest() + "----"
#         if not params.uuid:  # 默认使用文件名MD5值
#             params.uuid = stable_uuid
#         vector, token = extractor.extract_complete_features(params.pic_url)
#         core.token_manager.save_image_tokens(params.uuid, token)
#         core.milvus_lite.insert_vectors(vector, params.uuid,os.path.basename(params.pic_url))
#     return {"message": f"你提交的图片url是: {params.pic_url}, uuid是: {params.uuid}"}

# @app.post("/insert")
# def add(params: InsertParams):
#     '''
#     批量插入图片
#     输入json格式：
#     {
#         items: [
#             {
#                 uuid: "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
#                 pic_url: "xxx"
#             },
#             {
#                 uuid: "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
#                 pic_url: "xxx"
#             }
#         ]
#     }
#     '''
    
#     vectors = []
#     filenames = []
#     uuids = []
#     for item in params.items:
#         f = os.path.basename(item.pic_url)# 获取文件名
#         if f.lower().endswith((".png", ".jpg", ".jpeg")):
#             stable_uuid = hashlib.md5(f.encode("utf-8")).hexdigest() + "----"
#             if not item.uuid:  # 默认使用文件名MD5值
#                 item.uuid = stable_uuid
#             vector, token = extractor.extract_complete_features(item.pic_url)
#             core.token_manager.save_image_tokens(item.uuid, token)
#             uuids.append(item.uuid)  
#             vectors.append(vector)
#             filenames.append(os.path.basename(item.pic_url))
#     core.milvus_lite.insert_vectors(vectors,uuids,filenames)
#     return {"message": f"添加成功，共添加了 {len(params.items)} 张图片"}


if __name__ == "__main__":
    uvicorn.run(
        "main:app", host="127.0.0.1", port=8000, reload=False
    )  # 设置运行参数：网络地址 端口号 是否开启热更新
    
# uvicorn main:app --reload  开启热更新

# http://127.0.0.1:8000/docs
