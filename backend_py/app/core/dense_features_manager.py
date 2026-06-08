import os
import numpy as np
import torch
from pathlib import Path
import asyncio
from loguru import logger

# 定义 dense_features 缓存的根目录：项目根目录/data/dense_features/
PROJECT_ROOT = Path(__file__).resolve().parents[3]
DENSE_FEATURES_DIR = PROJECT_ROOT / "data" / "dense_features"
DENSE_FEATURES_DIR.mkdir(parents=True, exist_ok=True)


def save_image_dense_features(image_uuid, dense_features):
    # 1. 拼接完整的文件存储路径，例如：data/dense_features/0.npy
    file_path = DENSE_FEATURES_DIR / f"{image_uuid}.npy"
    
    # 2. 如果输入是 PyTorch Tensor，需要安全地将其转换成 NumPy 数组
    if isinstance(dense_features, torch.Tensor):
        # squeeze(0) 是为了防止带着 batch 维度 (1, 1370, 768)，强行平铺成标准的二维 [1370, 768]
        if dense_features.dim() == 3 and dense_features.size(0) == 1:
            dense_features = dense_features.squeeze(0)
        # 剥离梯度、转到 CPU、最后变换为标准的 float32 类型的 numpy 阵列
        dense_features_ndarray = dense_features.detach().cpu().numpy().astype(np.float32)
    else:
        dense_features_ndarray = np.asarray(dense_features, dtype=np.float32)
        if dense_features_ndarray.ndim == 3 and dense_features_ndarray.shape[0] == 1:
            dense_features_ndarray = dense_features_ndarray.squeeze(0)

    np.save(str(file_path), dense_features_ndarray)

async def save_image_dense_features_async(image_uuid, dense_features):
    """异步保存"""
    # 把同步操作丢到线程池
    await asyncio.to_thread(save_image_dense_features, image_uuid, dense_features)


def load_image_dense_features(image_uuid, device=None):
    file_path = DENSE_FEATURES_DIR / f"{image_uuid}.npy"
    
    # 健壮性检查：防止文件丢失或未提取直接读取
    if not file_path.exists():
        logger.error(f"找不到 dense_features 缓存文件 -> {file_path}")
        return None

    # 1. 使用底层磁盘映射机制闪电读取原始字节
    dense_features_ndarray = np.load(str(file_path)) # 此时形状为 [1370, 768]

    # 2. 如果指定了硬件设备（如 cuda），直接自动转换为共享内存的 PyTorch Tensor 并送上显卡
    if device is not None:
        dense_features_tensor = torch.from_numpy(dense_features_ndarray).to(device)
        return dense_features_tensor
        
    return dense_features_ndarray

async def load_image_dense_features_async(image_uuid, device=None):
    """异步加载"""
    # 把同步操作丢到线程池
    return await asyncio.to_thread(load_image_dense_features, image_uuid, device)