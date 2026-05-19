import timm
import torch
from PIL import Image
from io import BytesIO
import torchvision.transforms as transforms
import CosineSimilarityTool

# 1. 加载预训练 ViT 模型（你原本的代码）
device = torch.device("cuda") if torch.cuda.is_available() else torch.device("cpu")
model_vit = timm.create_model('vit_base_patch16_224', pretrained=True, num_classes=0).to(device)
model_vit.eval() # 设置模型为评估/推理模式

# 2. 准备图像预处理流程（ViT 模型通常要求的标准预处理）
transform = transforms.Compose([
    transforms.Resize(256),             # 先把图片短边缩放到 256
    transforms.CenterCrop(224),         # 再从中心裁剪出 224x224 的正方形
    transforms.ToTensor(),              # 转换为 Tensor 并归一化到 
    transforms.Normalize(mean=[0.5, 0.5, 0.5], std=[0.5, 0.5, 0.5]) # 标准化到 [-1, 1]
])

image_paths = [r"D:\EchoCampus\backend_py\app\temp_resources\services\d1.png", 
               r"D:\EchoCampus\backend_py\app\temp_resources\services\c1.jpg",
               r"D:\EchoCampus\backend_py\app\temp_resources\services\c2.png",
               r"D:\EchoCampus\backend_py\app\temp_resources\services\c3.png",
               r"D:\EchoCampus\backend_py\app\temp_resources\services\c4.png",
               r"D:\EchoCampus\backend_py\app\temp_resources\services\c5.png",
               ]
all_vectors = [] 

for image_path in image_paths:
    print(f"正在处理图片: {image_path}")
        
    # 1. 加载并预处理图片
    img = Image.open(image_path).convert('RGB')
    input_tensor = transform(img).unsqueeze(0).to(device)  # 增加 batch 维度 (1, 3, 224, 224)
    
    # 2. 进行推理（提取特征）
    with torch.no_grad():
        features = model_vit(input_tensor)  # 提取出的形状通常是 (1, 768)
        
    # 3. 将这张图片的特征添加到列表中
    one_d_vector = features.squeeze()  # 去掉 batch 维度，变成 (768,)
    all_vectors.append(one_d_vector)

similarity_matrix = CosineSimilarityTool.similarity_matrix(all_vectors)
print(f"相似度矩阵:\n{similarity_matrix}")  