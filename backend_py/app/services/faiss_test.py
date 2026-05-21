import sys
import os

# 获取当前文件(service)的上一级目录，也就是 backed_py 的绝对路径
PROJECT_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))

# 将项目根目录加入到 sys.path 的最前面
if PROJECT_ROOT not in sys.path:
    sys.path.insert(0, PROJECT_ROOT)

import faiss
import numpy as np
import core.select_pic as select_pic
import timm
import torch
from PIL import Image
from io import BytesIO
import torchvision.transforms as transforms


#读取索引文件
index = faiss.read_index(r'D:\EchoCampus\backend_py\app\temp_resources\image_database.index')

#选择图片
query_image_path = select_pic.select_single_file()

# 1. 加载预训练 ViT 模型（你原本的代码）
device = torch.device("cuda" if torch.cuda.is_available() else "cpu") # 自动检测是否有显卡
model_vit = timm.create_model('vit_base_patch16_224', pretrained=True, num_classes=0).to(device)
model_vit.eval() # 设置模型为评估/推理模式

# 2. 准备图像预处理流程（ViT 模型通常要求的标准预处理）
transform = transforms.Compose([
    transforms.Resize(256),             # 先把图片短边缩放到 256
    transforms.CenterCrop(224),         # 再从中心裁剪出 224x224 的正方形
    transforms.ToTensor(),              # 转换为 Tensor 并归一化到 
    transforms.Normalize(mean=[0.5, 0.5, 0.5], std=[0.5, 0.5, 0.5]) # 标准化到 [-1, 1]
])

print(f"正在处理图片: {query_image_path}")
    
# 1. 加载并预处理图片
img = Image.open(query_image_path).convert('RGB')
input_tensor = transform(img).unsqueeze(0).to(device)  # 增加 batch 维度 (1, 3, 224, 224)

# 2. 进行推理（提取特征）
with torch.no_grad():
    features = model_vit(input_tensor)  # 提取出的形状通常是 (1, 768)
    
# 3. 转为 numpy 数组并确保是 float32 类型（FAISS 的硬性要求）    
query_vector_np = features.cpu().numpy().astype('float32')

faiss.normalize_L2(query_vector_np)# 归一化查询向量，确保它也是单位向量

k = 5 # 搜索最相似的 5 张图片

# 执行搜索！
distances, indices = index.search(query_vector_np, k)

print("最相似图片的余弦相似度得分:", distances)
print("最相似图片在数据库中的下标:", indices)

