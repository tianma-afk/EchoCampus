import core.milvus_lite
import core.token_manager
from services.pair_vpr import PairVPRExtractor
import os
from pathlib import Path
import core.select_pic as select_pic
import time

extractor = PairVPRExtractor(model_type="vitB",use_fp16=True)
id = 0;

# 3. 获取文件夹下所有的图片路径（模拟 10000 张的场景）
image_folder = str(Path(__file__).resolve().parent/"temp_resources")
# 筛选出常见的图片格式
all_image_paths = [os.path.join(image_folder, f) for f in os.listdir(image_folder) 
                   if f.lower().endswith(('.png', '.jpg', '.jpeg'))]

print(f"共找到 {len(all_image_paths)} 张图片，开始提取特征...")

vectors = []
ids = []
for image_path in all_image_paths:
    vector , token = extractor.extract_complete_features(image_path)
    vectors.append(vector)
    ids.append(id)
    core.token_manager.save_image_tokens(id, token)
    id += 1
core.milvus_lite.insert_vectors(vectors, ids)

core.milvus_lite.load_collection()
print(f"特征提取完成，开始搜索相似图片...")

print("选择你要搜索的图片:")

goal_path = select_pic.select_single_file()
# 提取查询图片的完整特征（包括 tokens）

start_time = time.time()

goal_vector, goal_token = extractor.extract_complete_features(goal_path)

result = core.milvus_lite.search_similar(goal_vector)
print("搜索结果:")

# 批量加载候选 tokens
candidate_ids = [hit['id'] for hit in result]
candidate_tokens = [core.token_manager.load_image_tokens(img_id) for img_id in candidate_ids]

# 批量计算相似度（优化版）
scores = extractor.pair_similarity_batch([goal_token] * len(candidate_ids), candidate_tokens)

# 构建结果
results_with_scores = []
for hit, score in zip(result, scores):
    img_id = hit['id']
    filename = os.path.basename(all_image_paths[img_id])
    print(f"  - {filename} (ID: {img_id} , score: {hit['distance']})")
    results_with_scores.append((hit, score))

# 按 score 降序排序
results_with_scores.sort(key=lambda x: x[1], reverse=True)

# 打印排序后的结果
print("搜索结果（按相似度排序）:")
for hit, score in results_with_scores:
    img_id = hit['id']
    filename = os.path.basename(all_image_paths[img_id])
    print(f"  - {filename} (ID: {img_id}, score: {score})")

end_time = time.time()
print(f"搜索完成，耗时 {end_time - start_time} 秒")