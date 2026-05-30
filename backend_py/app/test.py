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
        stable_uuid_without_desh = hashlib.md5(f.encode('utf-8')).hexdigest()
        stable_uuid = stable_uuid_without_desh[0:8]+'-'+stable_uuid_without_desh[8:12]+'-'+stable_uuid_without_desh[12:16]+'-'+stable_uuid_without_desh[16:20]+'-'+stable_uuid_without_desh[20:32]
        all_image_paths[stable_uuid] = file_path


print(f"共找到 {len(all_image_paths)} 张图片，开始提取特征...")

vectors = []
uuids = []
filenames = []
for image_uuid, image_path in all_image_paths.items():
    vector, token = extractor.extract_complete_features(image_path)
    filename = os.path.basename(image_path)# 获取文件名
    filenames.append(filename)
    vectors.append(vector)
    uuids.append(image_uuid)
    core.token_manager.save_image_tokens(image_uuid, token)
print("检查uuids列表：")    
print(uuids)
# breakpoint()# 在这里检查 uuids 列表的内容，确保它们是字符串类型的 UUID
    
core.milvus_lite.insert_vectors(vectors, uuids,filenames)  # 批量插入向量、uuid 和文件名

core.milvus_lite.load_collection()
print(f"特征提取完成，开始搜索相似图片...")

print("选择你要搜索的图片:")

goal_path = select_pic.select_single_file()
# 提取查询图片的完整特征（包括 tokens）

start_time = time.time()

goal_vector, goal_token = extractor.extract_complete_features(goal_path)

result = core.milvus_lite.search_similar(goal_vector, top_k=10)
print("搜索结果:")

# 批量加载候选 tokens
candidate_ids = [hit['uuid'] for hit in result]
candidate_tokens = [core.token_manager.load_image_tokens(img_id) for img_id in candidate_ids]

# 批量计算相似度（优化版）
scores = extractor.pair_similarity_batch([goal_token] * len(candidate_ids), candidate_tokens)

# 构建结果
results_with_scores = []
for hit, score in zip(result, scores):
    img_uuid = hit['uuid']
    filename = hit["filename"]  # 直接从 result 中获取文件名
    print(f"  - {filename} (ID: {img_uuid} , score: {hit['score']})")
    results_with_scores.append((hit, score))

# 按 score 降序排序
results_with_scores.sort(key=lambda x: x[1], reverse=True)

# 打印排序后的结果
print("搜索结果（按相似度排序）:")
for hit, score in results_with_scores:
    img_uuid = hit['uuid']
    filename = hit["filename"]  # 直接从 result 中获取文件名
    print(f"  - {filename} (uuid: {img_uuid}, score: {score})")

end_time = time.time()
print(f"搜索完成，耗时 {end_time - start_time} 秒")