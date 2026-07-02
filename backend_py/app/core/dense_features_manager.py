import io
import socket
import numpy as np
import torch
import asyncio
import httpx
from datetime import timedelta
from typing import Optional
from loguru import logger
from minio import Minio
from minio.error import S3Error

from core.settings import settings

# ── IPv4 解析（避免 Windows 上 localhost 走 IPv6 超时） ───

def _resolve_ipv4(endpoint: str) -> str:
    host, _, port = endpoint.rpartition(":")
    port = port or "9000"
    try:
        addrs = socket.getaddrinfo(host, int(port), socket.AF_INET)
        if addrs:
            return f"{addrs[0][4][0]}:{port}"
    except socket.gaierror:
        pass
    return endpoint


# ── MinIO 客户端模块级单例 ──────────────────────────────

_minio_client: Optional[Minio] = None


def _get_minio_client() -> Minio:
    """延迟初始化 MinIO 客户端（与 milvus_service 同一模式）。"""
    global _minio_client
    if _minio_client is None:
        endpoint = _resolve_ipv4(settings.MINIO_ENDPOINT)
        _minio_client = Minio(
            endpoint=endpoint,
            access_key=settings.MINIO_ACCESS_KEY,
            secret_key=settings.MINIO_SECRET_KEY,
            secure=settings.MINIO_SECURE,
        )
    return _minio_client


def _object_key(image_uuid: str) -> str:
    """MinIO 对象键：tokens/{uuid}.npy"""
    return f"{settings.MINIO_DENSE_FEATURES_PREFIX}/{image_uuid}.npy"


# ── 公开 API（签名与旧版完全兼容）───────────────────────


def save_image_dense_features(image_uuid, dense_features):
    """将 dense_features 序列化为 .npy 并上传到 MinIO。"""
    # 1. Tensor / ndarray → float32 numpy，挤压 batch 维度
    if isinstance(dense_features, torch.Tensor):
        if dense_features.dim() == 3 and dense_features.size(0) == 1:
            dense_features = dense_features.squeeze(0)
        dense_features_ndarray = dense_features.detach().cpu().numpy().astype(np.float32)
    else:
        dense_features_ndarray = np.asarray(dense_features, dtype=np.float32)
        if dense_features_ndarray.ndim == 3 and dense_features_ndarray.shape[0] == 1:
            dense_features_ndarray = dense_features_ndarray.squeeze(0)

    # 2. 序列化到内存（不写磁盘）
    buf = io.BytesIO()
    np.save(buf, dense_features_ndarray)
    buf.seek(0)
    data_bytes = buf.getvalue()

    # 3. 上传到 MinIO
    client = _get_minio_client()
    client.put_object(
        bucket_name=settings.MINIO_BUCKET,
        object_name=_object_key(image_uuid),
        data=io.BytesIO(data_bytes),
        length=len(data_bytes),
        content_type="application/octet-stream",
    )


def load_image_dense_features(image_uuid, device=None):
    """从 MinIO 加载 dense_features .npy，缺失时返回 None。"""
    client = _get_minio_client()
    try:
        response = client.get_object(
            bucket_name=settings.MINIO_BUCKET,
            object_name=_object_key(image_uuid),
        )
        try:
            data = response.read()
            dense_features_ndarray = np.load(io.BytesIO(data))
        finally:
            response.close()
            response.release_conn()

        if device is not None:
            return torch.from_numpy(dense_features_ndarray).to(device)
        return dense_features_ndarray

    except S3Error as e:
        logger.error(f"MinIO 加载 dense_features 失败 [{image_uuid}]: {e}")
        return None


async def save_image_dense_features_async(image_uuid, dense_features):
    """异步保存（签名不变）。"""
    await asyncio.to_thread(save_image_dense_features, image_uuid, dense_features)


async def load_image_dense_features_async(image_uuid, device=None):
    """异步加载（签名不变）。"""
    return await asyncio.to_thread(load_image_dense_features, image_uuid, device)


async def load_image_dense_features_http_async(image_uuid, device=None):
    """异步 HTTP 预签名 URL 下载 dense_features .npy，绕过 sync MinIO client。"""
    object_name = _object_key(image_uuid)
    client = _get_minio_client()
    presigned_url = await asyncio.to_thread(
        client.presigned_get_object,
        settings.MINIO_BUCKET,
        object_name,
        expires=timedelta(minutes=5),
    )
    try:
        async with httpx.AsyncClient(timeout=5.0) as http_client:
            resp = await http_client.get(presigned_url)
            resp.raise_for_status()
            dense_features_ndarray = np.load(io.BytesIO(resp.content))
            if device is not None:
                return torch.from_numpy(dense_features_ndarray).to(device)
            return dense_features_ndarray
    except Exception as e:
        logger.error(f"HTTP 加载 dense_features 失败 [{image_uuid}]: {e}")
        return None
