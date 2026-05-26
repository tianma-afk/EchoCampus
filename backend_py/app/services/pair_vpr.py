import torch
import torch.nn.functional as F
import torchvision.transforms as transforms
from PIL import Image
import numpy as np
import os
from pathlib import Path
import requests
import sys
from omegaconf import OmegaConf
import re


sys.path.append(str(Path(__file__).resolve().parent))
from core.device import get_available_device, get_device, get_device_type, to_device, print_device_info

print(f"PyTorch版本: {torch.__version__}")
print_device_info()

BACKEND_PY_PATH = Path(__file__).resolve().parents[2]

# ==================== 添加官方仓库到路径 ====================
PAIR_VPR_ROOT = BACKEND_PY_PATH / "Pair-VPR"
if not PAIR_VPR_ROOT.exists():
    print(f"❌ 未找到官方仓库，请执行:")
    print(f"   cd {BACKEND_PY_PATH}")
    print(f"   git clone https://github.com/csiro-robotics/Pair-VPR.git")
    exit(1)

sys.path.insert(0, str(PAIR_VPR_ROOT))
print(f"✅ 已加载官方 Pair-VPR 库: {PAIR_VPR_ROOT}")

from pairvpr.models.pairvpr import PairVPRNet


# ==================== 配置加载 ====================
def get_cfg(model_type="vitB"):
    config_map = {
        "vitB": "stagetwo_default_config.yaml",
        "vitL": "stagetwo_default_config.yaml",
        "vitG": "stagetwo_default_config.yaml"
    }
    config_name = config_map.get(model_type, "stagetwo_default_config.yaml")
    config_path = PAIR_VPR_ROOT / "pairvpr/configs" / config_name
    
    if not config_path.exists():
        raise FileNotFoundError(f"配置文件不存在: {config_path}")
    
    cfg = OmegaConf.load(config_path)
    
    encoder_map = {
        "vitB": "dinov2_vitb14",
        "vitL": "dinov2_vitl14",
        "vitG": "dinov2_vitg14"
    }
    cfg.encoder.model_name = encoder_map.get(model_type, "dinov2_vitb14")
    cfg.globaldesc.dim = 512
    
    
    return cfg


# ==================== 官方权重加载器 ====================
class PairVPRExtractor:
    model_dict = {
        "vitB": {"path": "pairvpr-vitB.pth", "download_url": "https://huggingface.co/CSIRORobotics/Pair-VPR/resolve/main/pairvpr-vitB.pth"},
        "vitL": {"path": "pairvpr-vitL.pth", "download_url": "https://huggingface.co/CSIRORobotics/Pair-VPR/resolve/main/pairvpr-vitL.pth"},
        "vitG": {"path": "pairvpr-vitH.pth", "download_url": "https://huggingface.co/CSIRORobotics/Pair-VPR/resolve/main/pairvpr-vitG.pth"},
    }
    
    def __init__(self, model_type="vitB", processing_size=322, out_dim=512,use_fp16=False,
                 weight_dir=str(BACKEND_PY_PATH / "weights/vpr_weights")):
        self.model_type = model_type
        self.processing_size = processing_size
        self.device, self.device_type = get_available_device()
        self.weights_dir = weight_dir
        os.makedirs(weight_dir, exist_ok=True)
        
        self.model_path = os.path.join(self.weights_dir, self.model_dict[model_type]["path"])
        
        if not os.path.exists(self.model_path):
            self.download_file()
        
        cfg = get_cfg(model_type)
        cfg.globaldesc.dim = out_dim
        
        self.model = PairVPRNet(cfg)
        self._load_weights()

        
<<<<<<< HEAD
        if use_fp16 and self.device_type == 'xpu':
=======
        if use_fp16:
>>>>>>> ecf1b26975caa5db484dbecad6ea61e1e38e0730
            self.use_fp16 = use_fp16
            self.model = self.model.half()
            print("✨ 已启用 FP16 半精度推理")
        else:
            self.use_fp16 = False
<<<<<<< HEAD
            if use_fp16 and self.device_type != 'xpu':
                print(f"⚠️ FP16 仅在 xpu 上测试过，当前设备为 {self.device_type}，跳过")
=======
>>>>>>> ecf1b26975caa5db484dbecad6ea61e1e38e0730

        self.model.to(self.device)
        self.model.eval()

        # if self.device_type == 'directml' and hasattr(torch, 'compile'):
        #     self.model = torch.compile(self.model, backend="inductor")
        #     print("✨ 已启用 torch.compile 优化")
                
        # 🔥 关键：提取解码器组件（用于直接处理 tokens）
        self.decoder_embed = self.model.decoder_embed
        self.decoder_clstoken = self.model.decoder_clstoken
        self.decoder_pos_embed = self.model.dec_pos_embed
        self.dec_blocks = self.model.dec_blocks
        self.dec_norm = self.model.dec_norm
        self.classvprmodule = self.model.classvprmodule
        
        print(f"✨ 官方 Pair-VPR 部署成功！(模型: {model_type}, 特征维度: {out_dim})")
    
    def _load_weights(self):
        print(f"🔥 加载权重: {self.model_path}")
        state_dict = torch.load(self.model_path, map_location="cpu")
        
        if isinstance(state_dict, dict) and "state_dict" in state_dict:
            state_dict = state_dict["state_dict"]
        elif isinstance(state_dict, dict) and "model" in state_dict:
            state_dict = state_dict["model"]
        
        fixed_state_dict = {}
        for k, v in state_dict.items():
            new_key = k.replace("module.", "")
            # 修正历史命名：classvpr0/1/2 -> classvprmodule.0/1/2
            new_key = re.sub(r"^classvpr(\d+)\.", r"classvprmodule.\1.", new_key)
            fixed_state_dict[new_key] = v
        
        missing, unexpected = self.model.load_state_dict(fixed_state_dict, strict=False)
        if missing:
            print(f"⚠️ 缺失的键: {missing}")
        if unexpected:
            print(f"⚠️ 多余的键: {unexpected}")
        print(f"✅ 权重加载成功")
    
    def download_file(self):
        try:
            print(f"📥 下载权重: {self.model_dict[self.model_type]['path']}")
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
            print(f"\n✅ 下载完成: {self.model_path}")
        except Exception as e:
            print(f"❌ 下载失败: {e}")
            raise
    
    def _get_transform(self):
        return transforms.Compose([
            transforms.Resize(self.processing_size),
            transforms.CenterCrop((self.processing_size, self.processing_size)),
            transforms.ToTensor(),
            transforms.Normalize(mean=[0.485, 0.456, 0.406], std=[0.229, 0.224, 0.225])
        ])
    
    def extract_vector(self, image_path):
        """第一阶段：提取向量（给 Milvus）"""
        if not os.path.exists(image_path):
            return None
        
        transform = self._get_transform()
        img = Image.open(image_path).convert("RGB")
        img_tensor = transform(img).unsqueeze(0).to(self.device)
        
        with torch.no_grad():
            _, global_desc = self.model(img_tensor, None, mode="global")
            return global_desc.cpu().numpy().flatten().tolist()
    
    def extract_complete_features(self, image_path):
        """返回向量和 dense_features（缓存用）"""
        if not os.path.exists(image_path):
            return None, None
        
        transform = self._get_transform()
        img = Image.open(image_path).convert("RGB")
        img_tensor = transform(img).unsqueeze(0).to(self.device)

        if self.use_fp16:
            img_tensor = img_tensor.half()
        
        img_tensor = img_tensor.to(self.device)
        
        with torch.no_grad():
            dense_features, global_desc = self.model(img_tensor, None, mode="global")
            vector = global_desc.float().cpu().numpy().flatten().tolist()
            tokens = dense_features.float().squeeze(0).cpu()
        return vector, tokens
    
    def pair_similarity_from_cached_tokens(self, q_tokens, c_tokens):
        """
        q_tokens, c_tokens: 已经是 torch.Tensor 或 numpy.ndarray，形状为 [1, L, D] 或 [L, D]
        返回: float，相似度分数（raw score，越大越相似，行为上与官方 eval 保持一致）
        实现细节：对称得分 = model(q,c,'pairvpr') + model(c,q,'pairvpr')，始终返回 float（batch=1 的常见情况）。
        """
        # 转换为 torch.Tensor
        def ensure_tensor(x):
            if isinstance(x, np.ndarray):
                t = torch.from_numpy(x)
            elif isinstance(x, torch.Tensor):
                t = x
            else:
                t = torch.tensor(x)

            if self.use_fp16:
                return t.half()
            else:
                return t.float()

        q = ensure_tensor(q_tokens)
        c = ensure_tensor(c_tokens)

        # 形状规范化：如果是 [L, D] -> [1, L, D]
        if q.dim() == 2:
            q = q.unsqueeze(0)
        if c.dim() == 2:
            c = c.unsqueeze(0)

        # 广播 batch 大小（若一侧为1）
        if q.shape[0] == 1 and c.shape[0] > 1:
            q = q.expand(c.shape[0], -1, -1)
        if c.shape[0] == 1 and q.shape[0] > 1:
            c = c.expand(q.shape[0], -1, -1)

        # 移动到模型设备
        q = q.to(self.device)
        c = c.to(self.device)

        with torch.no_grad():
            # 使用官方 forward 的 pairvpr 分支以保持一致性
            s1 = self.model(q, c, mode="pairvpr")  # (B,1)
            s2 = self.model(c, q, mode="pairvpr")  # (B,1)
            scores = (s1 + s2).squeeze(-1)
            scores = scores.float().cpu()

        # 对常见的 batch=1 情况，始终返回 float
        if scores.numel() == 1:
            return float(scores.item())
        # 否则返回 list
        return scores.numpy().tolist()
    

if __name__ == "__main__":
    extractor = PairVPRExtractor(model_type="vitB")
    
    test_img = str(BACKEND_PY_PATH / "app/temp_resources/B1.jpg")
    if os.path.exists(test_img):
        vector, tokens = extractor.extract_complete_features(test_img)
        if vector:
            print(f"✅ 向量维度: {len(vector)}")
            print(f"✅ tokens 形状: {tokens.shape}")