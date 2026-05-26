from fastapi import FastAPI
import uvicorn
from pydantic import BaseModel # 接收时：它能一键把前端传来的 JSON 字典，直接变成你代码里可以点来点去的 Python 对象

import core.milvus_lite
import core.token_manager
from services.pair_vpr import PairVPRExtractor
import os
from pathlib import Path
import hashlib


app = FastAPI()
extractor = PairVPRExtractor(model_type="vitB",use_fp16=True)

@app.get("/")
def root():
    return {"message": "图搜图后端接口已启动！"}


# 1. 专门定义一个类，规定好要传哪两个数据
class SearchParams(BaseModel):
    pic_path: str  # 必须的字符串参数，表示图片路径
    top_k: int = 10  # 可选的整数参数，默认值为10
    
    
@app.post("/search")
def search(params: SearchParams):

    # 3. 获取文件夹下所有的图片路径（模拟 10000 张的场景）
    image_folder = str(Path(__file__).resolve().parent/"temp_resources")
    # 筛选出常见的图片格式
    all_image_paths = {}
    for f in os.listdir(image_folder):
        if f.lower().endswith((".png", ".jpg", ".jpeg")):
            file_path = os.path.join(image_folder, f)
            # 使用文件名的 MD5 值作为稳定的 UUID (保证是36位以内或适配你的长度限制)
            stable_id = hashlib.md5(f.encode('utf-8')).hexdigest()+"----"
            all_image_paths[stable_id] = file_path
    
    core.milvus_lite.load_collection()
    # 提取查询图片的完整特征（包括 tokens）
    goal_vector, goal_token = extractor.extract_complete_features(params.pic_path)

    result = core.milvus_lite.search_similar(goal_vector,params.top_k)
    # print("搜索结果:")
    results_with_scores = []
    json_results_1 = []
    for hit in result:  # Milvus client 返回 [[hit1, hit2, ...]]
        img_uuid = hit['id']
        filename = os.path.basename(all_image_paths[img_uuid])
        item = {
            "filename": filename,
            "uuid": img_uuid,
            "score": hit['score']  
        }
        json_results_1.append(item)
        score = extractor.pair_similarity_from_cached_tokens(goal_token, core.token_manager.load_image_tokens(img_uuid))
        results_with_scores.append((hit, score))

    # 按 score 降序排序
    results_with_scores.sort(key=lambda x: x[1], reverse=True)

    # 打印排序后的结果
    json_results_2 = []
    # print("搜索结果（按相似度排序）:")
    for hit, score in results_with_scores:
        img_uuid = hit['id']
        filename = os.path.basename(all_image_paths[img_uuid])
        item = {
            "filename": filename,
            "uuid": img_uuid,
            "score": score  
        }
        json_results_2.append(item)
    return {
        "message": "图搜图成功",
        "results_1":json_results_1,      # 初始比较的结果
        "results_2": json_results_2      # 筛选后的结果
    }
    
    
class InsertParams(BaseModel):
    pic_url: str
    uuid : str #36位
    
@app.post("/insert")    
def add(params: InsertParams):
    
    return {"message": f"你提交的图片url是: {params.pic_url}"}



if __name__ == "__main__":
    uvicorn.run("main:app", host="127.0.0.1", port=8000, reload=False)#设置运行参数：网络地址 端口号 是否开启热更新
# uvicorn main:app --reload  开启热更新

# http://127.0.0.1:8000/docs
