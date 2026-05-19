import os
import timm
import torch
import numpy as np
from PIL import Image
import faiss
import torchvision.transforms as transforms

# 1. 加载预训练 ViT 模型
device = torch.device("cuda" if torch.cuda.is_available() else "cpu") # 自动检测是否有显卡
model_vit = timm.create_model('vit_base_patch16_224', pretrained=True, num_classes=0).to(device)
model_vit.eval()

# 2. 图像预处理流程（和你原本的完全一致）
transform = transforms.Compose([
    transforms.Resize(256),
    transforms.CenterCrop(224),
    transforms.ToTensor(),
    transforms.Normalize(mean=[0.5, 0.5, 0.5], std=[0.5, 0.5, 0.5])
])

# 3. 获取文件夹下所有的图片路径（模拟 10000 张的场景）
image_folder = r"D:\EchoCampus\backend_py\app\temp_resources"
# 筛选出常见的图片格式
all_image_paths = [os.path.join(image_folder, f) for f in os.listdir(image_folder) 
                   if f.lower().endswith(('.png', '.jpg', '.jpeg'))]

print(f"共找到 {len(all_image_paths)} 张图片，开始提取特征...")

# 4. 批量提取特征并存入 NumPy 数组
all_vectors = []
batch_size = 32  # 每次让显卡处理 32 张图，速度更快

for i in range(0, len(all_image_paths), batch_size):
    batch_paths = all_image_paths[i : i + batch_size]
    batch_tensors = []
    
    for path in batch_paths:
        try:
            img = Image.open(path).convert('RGB')
            input_tensor = transform(img)
            batch_tensors.append(input_tensor)
        except Exception as e:
            print(f"读取图片失败 {path}: {e}")

    if batch_tensors:
        # 拼成一个大的 batch (32, 3, 224, 224) 并放到 GPU/CPU 上
        batch_input = torch.stack(batch_tensors).to(device)
        
        with torch.no_grad():
            features = model_vit(batch_input) # 一次提取 32 张
        
        # 转移到 CPU 并转为 numpy 数组，存入总列表
        all_vectors.append(features.cpu().numpy())


# 5. 将所有批次的结果拼接成一个完整的 NumPy 矩阵 (10000, 768)
features_matrix = np.concatenate(all_vectors, axis=0).astype('float32')

# 6. 对拼接后的完整特征矩阵进行 L2 归一化
# 直接对 features_matrix 进行操作，确保存入数据库的是单位向量
faiss.normalize_L2(features_matrix)

print(f"特征提取完成！最终矩阵形状: {features_matrix.shape}")

# 直接使用处理好的真实特征矩阵
dimension = features_matrix.shape[1] # 维度是 768

# 建立内积索引 (IndexFlatIP)
# 配合归一化后的向量，内积(IP)就等于余弦相似度。
# 之前用的 IndexFlatL2 是算欧氏距离的（数值越小越相似），这里改成 IP（数值越大越相似）
index = faiss.IndexFlatIP(dimension) 
index.add(features_matrix) # 把归一化后的真实向量存进去

print(f"FAISS 数据库构建成功，当前共有 {index.ntotal} 张图片待检索！")

# 保存索引
faiss.write_index(index, r"D:\EchoCampus\backend_py\app\temp_resources\image_database.index")
