import core.milvus_lite
import core.token_manager
from services.pair_vpr import PairVPRExtractor
import os
from pathlib import Path
import core.select_pic as select_pic
import uuid
import hashlib
import time

extractor = PairVPRExtractor(model_type="vitB",use_fp16=True)


# 3. 获取文件夹下所有的图片路径（模拟 10000 张的场景）
image_folder = str(Path(__file__).resolve().parent / "temp_resources")
# 筛选出常见的图片格式
all_image_paths = {}
for f in os.listdir(image_folder):
    if f.lower().endswith((".png", ".jpg", ".jpeg")):
        file_path = os.path.join(image_folder, f)
        # 使用文件名的 MD5 值作为稳定的 UUID (保证是36位以内或适配你的长度限制)
        stable_id = hashlib.md5(f.encode('utf-8')).hexdigest()+"----"
        all_image_paths[stable_id] = file_path

print(f"共找到 {len(all_image_paths)} 张图片，开始提取特征...")

vectors = []
ids = []
filenames = []
for image_uuid, image_path in all_image_paths.items():
    vector, token = extractor.extract_complete_features(image_path)
    filename = os.path.basename(image_path)# 获取文件名
    filenames.append(filename)
    vectors.append(vector)
    ids.append(image_uuid)
    core.token_manager.save_image_tokens(image_uuid, token)
print("检查ids列表：")    
print(ids)
# breakpoint()# 在这里检查 ids 列表的内容，确保它们是字符串类型的 UUID
    
core.milvus_lite.insert_vectors(vectors, ids,filenames)  # 批量插入向量、ID 和文件名

core.milvus_lite.load_collection()
print(f"特征提取完成，开始搜索相似图片...")

print("选择你要搜索的图片:")

goal_path = select_pic.select_single_file()
# 提取查询图片的完整特征（包括 tokens）

start_time = time.time()

goal_vector, goal_token = extractor.extract_complete_features(goal_path)

result = core.milvus_lite.search_similar(goal_vector, top_k=10)
print("搜索结果:")
results_with_scores = []
for hit in result:  # Milvus client 返回 [[hit1, hit2, ...]]
    img_uuid = hit["id"]
    filename = hit["filename"]  # 直接从 result 中获取文件名
    print(f"  - {filename} (ID: {img_uuid} , score: {hit['score']})")
    score = extractor.pair_similarity_from_cached_tokens(
        goal_token, core.token_manager.load_image_tokens(img_uuid)
    )
    results_with_scores.append((hit, score))

# 按 score 降序排序
results_with_scores.sort(key=lambda x: x[1], reverse=True)

# 打印排序后的结果
print("搜索结果（按相似度排序）:")
for hit, score in results_with_scores:
    img_id = hit['id']
    filename = hit['filename']
    print(f"  - {filename} (ID: {img_id}, score: {score})")

end_time = time.time()
print(f"搜索完成，耗时 {end_time - start_time} 秒")
