import timm
import torch
from PIL import Image
from io import BytesIO
import torchvision.transforms as transforms

# 1. 加载预训练 ViT 模型（你原本的代码）
model_vit = timm.create_model('vit_base_patch16_224', pretrained=True, num_classes=0)
model_vit.eval() # 设置模型为评估/推理模式

# 2. 准备图像预处理流程（ViT 模型通常要求的标准预处理）
transform = transforms.Compose([
    transforms.Resize(256),             # 先把图片短边缩放到 256
    transforms.CenterCrop(224),         # 再从中心裁剪出 224x224 的正方形
    transforms.ToTensor(),              # 转换为 Tensor 并归一化到 
    transforms.Normalize(mean=[0.5, 0.5, 0.5], std=[0.5, 0.5, 0.5]) # 标准化到 [-1, 1]
])

# 3. 获取一张测试图片（这里直接通过网络加载一张网上的猫咪图片）
img = Image.open(r"D:\EchoCampus\backend_py\app\test.png").convert('RGB')

# 4. 对图片进行预处理，并增加 batch 维度 (因为模型一次通常处理一批图片)
input_tensor = transform(img).unsqueeze(0)  # 形状变为 (1, 3, 224, 224)

# 5. 进行推理（提取特征）
with torch.no_grad():  # 推理时关闭梯度计算，节省内存并加速
    features = model_vit(input_tensor)

# 6. 打印结果，看看提取出的特征长什么样
print(f"提取到的特征向量形状: {features.shape}")
print(f"特征向量的前5个数值: {features[:5]}")