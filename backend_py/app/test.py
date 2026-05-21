import core.milvus_lite
from services.pair_vpr import PairVPRExtractor
import os
from pathlib import Path
import core.select_pic as select_pic

extractor = PairVPRExtractor(model_type="vitL")
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
    vectors.append(extractor.extract_vector(image_path))
    ids.append(id)
    id += 1
core.milvus_lite.insert_vectors(vectors, ids)

print(f"特征提取完成，开始搜索相似图片...")

print("选择你要搜索的图片:")

goal_path = select_pic.select_single_file()
goal_vector = extractor.extract_vector(goal_path)

result = core.milvus_lite.search_similar(goal_vector)
print("搜索结果:")
for hit in result:  # Milvus client 返回 [[hit1, hit2, ...]]
    img_id = hit['id']
    score = hit['distance']
    filename = os.path.basename(all_image_paths[img_id])  # 只显示文件名
    print(f"  - {filename} (ID: {img_id}, 相似度: {score:.4f})")
