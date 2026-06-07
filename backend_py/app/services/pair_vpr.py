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
from loguru import logger


sys.path.append(str(Path(__file__).resolve().parent))
from core.device import get_available_device, get_device, get_device_type, to_device, get_device_info

logger.info(f"PyTorch 版本: {torch.__version__}")
logger.info(f"设备信息: {get_device_info()}")

BACKEND_PY_PATH = Path(__file__).resolve().parents[2]

# ==================== 添加官方仓库到路径 ====================
PAIR_VPR_ROOT = BACKEND_PY_PATH / "app/vendors/pairvpr"
if not PAIR_VPR_ROOT.exists():
    logger.error(f"未找到官方仓库 Pair-VPR: {PAIR_VPR_ROOT}")
    exit(1)
from vendors.pairvpr.models.pairvpr import PairVPRNet
logger.info(f"已加载官方 Pair-VPR 库: {PAIR_VPR_ROOT}")


# ==================== 配置加载 ====================
def get_cfg(model_type="vitB"):
    config_map = {
        "vitB": "stagetwo_default_config.yaml",
        "vitL": "stagetwo_default_config.yaml",
        "vitG": "stagetwo_default_config.yaml"
    }
    config_name = config_map.get(model_type, "stagetwo_default_config.yaml")
    config_path = PAIR_VPR_ROOT / "configs" / config_name
    
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

        
        if use_fp16:
            self.use_fp16 = use_fp16
            self.model = self.model.half()
            logger.info("已启用 FP16 半精度推理")
        else:
            self.use_fp16 = False

        self.model.to(self.device)
        self.model.eval()
                
        # 🔥 关键：提取解码器组件（用于直接处理 tokens）
        self.decoder_embed = self.model.decoder_embed
        self.decoder_clstoken = self.model.decoder_clstoken
        self.decoder_pos_embed = self.model.dec_pos_embed
        self.dec_blocks = self.model.dec_blocks
        self.dec_norm = self.model.dec_norm
        self.classvprmodule = self.model.classvprmodule
        
        logger.info(f"Pair-VPR 部署成功 (模型: {model_type}, 特征维度: {out_dim})")
    
    def _load_weights(self):
        logger.debug(f"加载权重: {self.model_path}")
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
            logger.warning(f"权重缺失的键: {missing}")
        if unexpected:
            logger.warning(f"权重多余的键: {unexpected}")
        logger.info("权重加载成功")
    
    def download_file(self):
        try:
            logger.info(f"下载权重: {self.model_dict[self.model_type]['path']}")
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
                            logger.debug(f"下载进度: {percent:.1f}%")
            logger.info(f"下载完成: {self.model_path}")
        except Exception as e:
            logger.error(f"下载失败: {e}")
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
    
    def extract_complete_features(self, image):
        """返回向量和 dense_features（缓存用）"""
        if image is None:
            return None, None
        
        transform = self._get_transform()
        img_tensor = transform(image).unsqueeze(0).to(self.device)

        if self.use_fp16:
            img_tensor = img_tensor.half()
        
        img_tensor = img_tensor.to(self.device)
        
        with torch.no_grad():
            dense_features, global_desc = self.model(img_tensor, None, mode="global")
            vector = global_desc.float().cpu().numpy().flatten().tolist()
            tokens = dense_features.float().squeeze(0).cpu()
        return vector, tokens
    
    def ensure_tensor(self,x):
        if isinstance(x, np.ndarray):
            t = torch.from_numpy(x)
        elif isinstance(x, torch.Tensor):
            t = x
        else:
            t = torch.tensor(x)
        return t.half() if self.use_fp16 else t.float()

    
    def pair_similarity_from_cached_tokens(self, q_tokens, c_tokens):
        """
        q_tokens, c_tokens: 已经是 torch.Tensor 或 numpy.ndarray，形状为 [1, L, D] 或 [L, D]
        返回: float，相似度分数（raw score，越大越相似，行为上与官方 eval 保持一致）
        实现细节：对称得分 = model(q,c,'pairvpr') + model(c,q,'pairvpr')，始终返回 float（batch=1 的常见情况）。
        """
        # 转换为 torch.Tensor
        
        q = self.ensure_tensor(q_tokens)
        c = self.ensure_tensor(c_tokens)

        # 形状规范化
        if q.dim() == 2:
            q = q.unsqueeze(0)
        if c.dim() == 2:
            c = c.unsqueeze(0)

        # 移到设备一次
        q = q.to(self.device)
        c = c.to(self.device)

        with torch.no_grad():
            # 对称计算（两个方向）
            s1 = self.model(q, c, mode="pairvpr")
            s2 = self.model(c, q, mode="pairvpr")
            score = (s1 + s2).squeeze(-1)
            score = score.float().cpu()

        return float(score.item()) if score.numel() == 1 else score.numpy().tolist()

    def pair_similarity_batch(self, q_tokens_list, c_tokens_list):
        """
        向量化批量比对：尽量将多对 (q, c) 一次性堆成 [B, L, D] 调用 model，以减少 Python 循环与设备切换开销。
        如果输入的 token 形状不一致（无法 stack），回退到逐对循环实现以保证兼容性。

        q_tokens_list: list of Tensor/ndarray，每个形状 [1, L, D] 或 [L, D]
        c_tokens_list: list of Tensor/ndarray，每个形状 [1, L, D] 或 [L, D]

        返回: list of float，每个是相应对的相似度
        """

        if len(q_tokens_list) != len(c_tokens_list):
            raise ValueError("q_tokens_list and c_tokens_list must have the same length")

        q_tensors = []
        c_tensors = []
        for qt, ct in zip(q_tokens_list, c_tokens_list):
            q = self.ensure_tensor(qt)
            c = self.ensure_tensor(ct)

            # 仅支持每项为 [L, D] 或 [1, L, D] 的情况；若为 [1, L, D] 则 squeeze
            if q.dim() == 3:
                if q.shape[0] == 1:
                    q = q.squeeze(0)
                else:
                    raise RuntimeError("cannot vectorize: q item has batch>1")
            if c.dim() == 3:
                if c.shape[0] == 1:
                    c = c.squeeze(0)
                else:
                    raise RuntimeError("cannot vectorize: c item has batch>1")

            if q.dim() != 2 or c.dim() != 2:
                raise RuntimeError("cannot vectorize: unexpected tensor dims")

            q_tensors.append(q)
            c_tensors.append(c)

        # stack -> [B, L, D]
        q_batch = torch.stack(q_tensors, dim=0).to(self.device)
        c_batch = torch.stack(c_tensors, dim=0).to(self.device)


        # 成功构建批次后一次性计算
        with torch.no_grad():
            s1 = self.model(q_batch, c_batch, mode="pairvpr")
            s2 = self.model(c_batch, q_batch, mode="pairvpr")
            score = (s1 + s2).squeeze(-1).float().cpu()

            # 返回 Python list
            if score.dim() == 0:
                return [float(score.item())]
            return score.numpy().tolist()
    
    def pair_similarity_batch_single_query(self, q_tokens, c_tokens_list):
        return self.pair_similarity_batch([q_tokens] * len(c_tokens_list), c_tokens_list)