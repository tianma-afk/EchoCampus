import os
import numpy as np
import torch
from pathlib import Path
import asyncio
from loguru import logger

# 定义 tokens 缓存的根目录：项目根目录/data/tokens/
PROJECT_ROOT = Path(__file__).resolve().parents[3]
TOKENS_DIR = PROJECT_ROOT / "data" / "tokens"
TOKENS_DIR.mkdir(parents=True, exist_ok=True)


def save_image_tokens(image_uuid, tokens):
    # 1. 拼接完整的文件存储路径，例如：data/tokens/0.npy
    file_path = TOKENS_DIR / f"{image_uuid}.npy"
    
    # 2. 如果输入是 PyTorch Tensor，需要安全地将其转换成 NumPy 数组
    if isinstance(tokens, torch.Tensor):
        # squeeze(0) 是为了防止带着 batch 维度 (1, 1370, 768)，强行平铺成标准的二维 [1370, 768]
        if tokens.dim() == 3 and tokens.size(0) == 1:
            tokens = tokens.squeeze(0)
        # 剥离梯度、转到 CPU、最后变换为标准的 float32 类型的 numpy 阵列
        tokens_ndarray = tokens.detach().cpu().numpy().astype(np.float32)
    else:
        tokens_ndarray = np.asarray(tokens, dtype=np.float32)
        if tokens_ndarray.ndim == 3 and tokens_ndarray.shape[0] == 1:
            tokens_ndarray = tokens_ndarray.squeeze(0)

    np.save(str(file_path), tokens_ndarray)

async def save_image_tokens_async(image_uuid, tokens):
    """异步保存"""
    # 把同步操作丢到线程池
    await asyncio.to_thread(save_image_tokens, image_uuid, tokens)


def load_image_tokens(image_uuid, device=None):
    file_path = TOKENS_DIR / f"{image_uuid}.npy"
    
    # 健壮性检查：防止文件丢失或未提取直接读取
    if not file_path.exists():
        logger.error(f"找不到 Token 缓存文件 -> {file_path}")
        return None

    # 1. 使用底层磁盘映射机制闪电读取原始字节
    tokens_ndarray = np.load(str(file_path)) # 此时形状为 [1370, 768]

    # 2. 如果指定了硬件设备（如 cuda），直接自动转换为共享内存的 PyTorch Tensor 并送上显卡
    if device is not None:
        tokens_tensor = torch.from_numpy(tokens_ndarray).to(device)
        return tokens_tensor
        
    return tokens_ndarray

async def load_image_tokens_async(image_uuid, device=None):
    """异步加载"""
    # 把同步操作丢到线程池
    return await asyncio.to_thread(load_image_tokens, image_uuid, device)