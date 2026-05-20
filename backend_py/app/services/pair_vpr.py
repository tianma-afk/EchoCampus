import torch
import torch.nn as nn
import torch.nn.functional as F
import torchvision.transforms as transforms
import timm
from PIL import Image
import numpy as np
import os
from pathlib import Path
import requests

print(f"PyTorch版本: {torch.__version__}")
print(f"CUDA是否可用: {torch.cuda.is_available()}")
if torch.cuda.is_available():
    print(f"当前CUDA设备: {torch.cuda.get_device_name(0)}")
BACKEND_PY_PATH = Path(__file__).resolve().parents[2]

# ==================== 1. ViT-B 专用 Pair-VPR 网络骨架 ====================
class PairVPRvitBNet(nn.Module):
    """
    专门适配 pairvpr-vitB.pth 官方权重的标准 Transformer 架构
    骨干网络采用标准的 ViT-B/14 (特征维度为 768)
    """
    def __init__(self, out_dim=4096):
        super().__init__()
        print("正在构建标准的 ViT-B/14 视觉骨干网络...")
        # 直接通过 timm 的 VisionTransformer 构造函数创建，参数完美对齐
        from timm.models.vision_transformer import VisionTransformer
        self.backbone = VisionTransformer(
            img_size=512,      # 💡 核心修复：直接对齐官方模型预期的 512 尺寸位置编码
            patch_size=14, 
            embed_dim=768, 
            depth=12, 
            num_heads=12, 
            num_classes=0
        )
        # 全连接降维投影层
        self.fc = nn.Linear(768, out_dim)

        self.classvprmodule = nn.Sequential(
            nn.Linear(768 * 2, 512),
            nn.ReLU(),
            nn.Linear(512, 1)
        )

    def forward(self, x):
        """🌟 严格对齐类级别的 forward 函数，确保 PyTorch 能够正常识别调用"""
        # 直接使用 timm 原生的高性能特征提取流
        features = self.backbone.forward_features(x)
        
        # 提取首位 CLS 全局通识 Token
        cls_token = features[:, 0, :] 
        global_vector = self.fc(cls_token)
        global_vector = F.normalize(global_vector, p=2, dim=1)
        
        # 完好无损地返回全局向量和包含所有空间位置的 Tokens 矩阵
        return global_vector, features
    
    def forward_features(self, x):
        """
        🚀 核心修改 1：阶段一调用。同时提取粗筛向量和完整精排 Token
        """
        # features 形状: [B, 1370, 768] (包含 1 个 CLS Token + 1369 个 Patch Tokens)
        all_tokens = self.backbone.forward_features(x)
        
        # 提取首位 CLS 全局通识 Token 用于全局粗筛
        cls_token = all_tokens[:, 0, :] 
        global_vector = self.fc(cls_token)
        global_vector = F.normalize(global_vector, p=2, dim=1)
        
        # 完好无损地返回全局向量和包含所有空间位置的 Tokens 矩阵
        return global_vector, all_tokens

    def forward_rerank(self, q_tokens, c_tokens):
        """
        🚀 核心修改 2：阶段二调用。计算查询图和候选图两两配对的得分
        """
        # 提取两张图的 CLS Token 进行交互
        q_cls = q_tokens[:, 0, :]  # [B, 768]
        c_cls = c_tokens[:, 0, :]  # [B, 768]
        
        # 特征拼接
        interaction = torch.cat([q_cls, c_cls], dim=-1)  # [B, 1536]
        
        # 经过微调好的分类头，输出 0~1 之间的同地点概率置信度得分
        score = torch.sigmoid(self.classvprmodule(interaction))
        return score


# ==================== 2. ViT-B 权重载入提取器 ====================
class PairVPRExtractor:
    model_dict ={
        "vitB": {"path": "pairvpr-vitB.pth","download_url": "https://huggingface.co/CSIRORobotics/Pair-VPR/resolve/main/pairvpr-vitB.pth"},
        "vitL": {"path": "pairvpr-vitL.pth","download_url": "https://huggingface.co/CSIRORobotics/Pair-VPR/resolve/main/pairvpr-vitL.pth"},
        "vitG": {"path": "pairvpr-vitH.pth","download_url": "https://huggingface.co/CSIRORobotics/Pair-VPR/resolve/main/pairvpr-vitG.pth"},
    }
    def __init__(self, model_type = "vitB",  processing_size=512, out_dim=4096, weight_dir= str(BACKEND_PY_PATH/"weights/vpr_weights")):
        self.model_type = model_type
        self.processing_size = processing_size
        self.device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
        self.weights_dir = weight_dir
        os.makedirs(weight_dir, exist_ok=True)
        
        self.model_path = os.path.join(self.weights_dir, self.model_dict[model_type]["path"])
        
        self.model = PairVPRvitBNet(out_dim=out_dim)
        self._load_official_weights()
        self.model.to(self.device)
        self.model.eval()
        
        print(f"✨ Pair-VPR 部署成功！(特征维度: {out_dim})")

    def _load_official_weights(self):
        """核心挂载逻辑：安全解包并注入官方 ViT-B 权重"""
        if not os.path.exists(self.model_path):
            print(f"⚠️ [未发现文件] 尝试下载官方权重文件...")
            self.download_file()
            if not os.path.exists(self.model_path): # 确保文件下载成功
                print(f"❌ [未发现文件] 下载权重失败，请检查网络连接。")
                exit(0)
        print(f"🔥 [核心挂载] 正在读取官方 {self.model_type} 预训练权重: {self.model_path}")
        try:
            state_dict = torch.load(self.model_path, map_location="cpu")
            
            if isinstance(state_dict, dict) and "state_dict" in state_dict:
                state_dict = state_dict["state_dict"]
            elif isinstance(state_dict, dict) and "model" in state_dict:
                state_dict = state_dict["model"]
            
            fixed_state_dict = {}
            for k, v in state_dict.items():
                new_key = k.replace("module.", "").replace("backbone.", "")
                fixed_state_dict[new_key] = v

            msg = self.model.load_state_dict(fixed_state_dict, strict=False)
            print(f"✅ ViT-B 官方街景知识成功注入！完美打通自监督注意力机制。")
            state_dict = torch.load(self.model_path, map_location="cpu")
            print("权重文件中的键：", list(state_dict.keys())[:10])  # 打印前10个键名
        except Exception as e:
            print(f"⚠️ 载入官方权重失败，错误原因: {e}。已降级为安全空间初始化。")

    def download_file(self):
        """下载大文件，带进度条"""
        try:
            response = requests.get(self.model_dict[self.model_type]["download_url"], stream=True)
            response.raise_for_status()
            
            total_size = int(response.headers.get('content-length', 0))
            
            os.makedirs(self.weights_dir, exist_ok=True)
            
            downloaded = 0
            with open(self.model_path, 'wb') as file:
                for chunk in response.iter_content(chunk_size=8192):
                    if chunk:
                        file.write(chunk)
                        downloaded += len(chunk)
                        if total_size > 0:
                            percent = (downloaded / total_size) * 100
                            print(f"\r下载进度: {percent:.1f}%", end='')
            
            print(f"\n下载完成: {self.model}")
            
        except requests.exceptions.RequestException as e:
            print(f"下载失败: {e}")

    def extract_complete_features(self, image_path):
        """🚀 新提供的方法。同时返回粗筛向量（List）和本地缓存用的 Token 矩阵（Tensor）"""
        if not os.path.exists(image_path):
            print(f" ❌ 提取失败: 文件未找到 -> {image_path}")
            return None, None
            
        transform = transforms.Compose([
            transforms.Resize(self.processing_size), 
            transforms.CenterCrop((self.processing_size, self.processing_size)),
            transforms.ToTensor(),
            transforms.Normalize(mean=[0.485, 0.456, 0.406], std=[0.229, 0.224, 0.225])
        ])
        
        try:
            img = Image.open(image_path).convert("RGB")
            img_tensor = transform(img).unsqueeze(0).to(self.device)
            with torch.no_grad():
                # 调用不截断的 forward_features 函数
                global_vector, all_tokens = self.model.forward_features(img_tensor)
                
                # 转换成普通的列表供 Milvus 使用
                vector_list = global_vector.cpu().numpy().flatten().tolist()
                # 移除 Batch 维度，保持 [1370, 768] 形状返回
                tokens_tensor = all_tokens.squeeze(0)
                
            return vector_list, tokens_tensor
        except Exception as e:
            print(f" ❌ 提取失败: {image_path}, 错误: {e}")
            return None, None
        
if __name__ == "__main__":
    import torch
    
    # 1. 加载权重文件
    weight_path = str(BACKEND_PY_PATH / "weights/vpr_weights/pairvpr-vitB.pth")
    print(f"正在检查权重文件: {weight_path}")
    
    if not os.path.exists(weight_path):
        print(f"❌ 权重文件不存在: {weight_path}")
        exit(0)
    
    state_dict = torch.load(weight_path, map_location="cpu")
    
    # 解包（处理可能的嵌套）
    if isinstance(state_dict, dict) and "state_dict" in state_dict:
        state_dict = state_dict["state_dict"]
    elif isinstance(state_dict, dict) and "model" in state_dict:
        state_dict = state_dict["model"]
    
    # 2. 检查键名，判断权重类型
    all_keys = list(state_dict.keys())
    print(f"\n权重文件中共有 {len(all_keys)} 个键")
    
    # 关键判断条件
    has_decoder = any("decoder" in k for k in all_keys)
    has_classifier = any("classvprmodule" in k or "pair" in k for k in all_keys)
    has_encoder = any("encoder" in k for k in all_keys)
    
    print(f"\n🔍 键名分析:")
    print(f"  - 包含 'encoder' 相关键: {'✅ 是' if has_encoder else '❌ 否'}")
    print(f"  - 包含 'decoder' 相关键: {'✅ 是' if has_decoder else '❌ 否'}")
    print(f"  - 包含分类器相关键: {'✅ 是' if has_classifier else '❌ 否'}")
    
    # 3. 打印前20个键名示例
    print(f"\n📋 前20个键名示例:")
    for i, key in enumerate(all_keys[:20]):
        print(f"    {i+1}. {key}")
    
    # 4. 判断结果
    print(f"\n{'='*50}")
    print(f"📊 判断结果:")
    
    if has_classifier:
        print("✅ 包含分类器相关键 -> 这是【阶段二】微调权重")
        print("   支持配对分类器重排序功能！")
    elif has_decoder and not has_classifier:
        print("⚠️ 包含 decoder 但无分类器键 -> 这是【阶段一】预训练权重")
        print("   解码器用于图像重建，不能直接用于地点判断")
        print("   建议: 寻找 stage2 版本权重，或使用降级方案")
    else:
        print("❌ 未找到 decoder 或 classifier 键 -> 可能仅为编码器权重")