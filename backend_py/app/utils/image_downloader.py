# services/image_downloader.py
import httpx
from io import BytesIO
from PIL import Image
from typing import Optional, Union
import asyncio
import re
from loguru import logger

from core.settings import settings

# Docker 网络内 MinIO 访问：Java 后端传的 URL 可能是 http://localhost:9000/...
# 容器内需要改写为 Docker 服务名 http://minio:9000/...
_MINIO_INTERNAL_HOST = settings.MINIO_INTERNAL_HOST


def _rewrite_minio_url(url: str) -> str:
    """将外部 MinIO URL（如 localhost:9000）改写为 Docker 内部服务地址"""
    if _MINIO_INTERNAL_HOST:
        rewritten = re.sub(r'https?://[^/]+', f'http://{_MINIO_INTERNAL_HOST}', url)
        if rewritten != url:
            logger.debug(f"MinIO URL 重写: {url} → {rewritten}")
        return rewritten
    return url


class AsyncImageDownloader:
    """异步图片下载服务"""

    def __init__(self, timeout: int = 10, max_size: Optional[int] = None):
        self.timeout = timeout
        self.max_size = max_size

    async def download_to_pil(self, url: str) -> Optional[Image.Image]:
        """异步下载并转换为RGB模式"""
        url = _rewrite_minio_url(url)
        async with httpx.AsyncClient(timeout=self.timeout) as client:
            try:
                response = await client.get(url)
                response.raise_for_status()
                
                # 检查大小限制
                if self.max_size and len(response.content) > self.max_size:
                    return None
                
                # 转换为RGB模式
                img = Image.open(BytesIO(response.content))
                return img.convert("RGB")
                
            except Exception as e:
                logger.error(f"下载失败 {url}: {e}")
                return None
    
    async def download_to_bytes(self, url: str) -> Optional[BytesIO]:
        """异步下载到字节流"""
        async with httpx.AsyncClient(timeout=self.timeout) as client:
            try:
                response = await client.get(url)
                response.raise_for_status()
                
                if self.max_size and len(response.content) > self.max_size:
                    return None
                
                return BytesIO(response.content)
                
            except Exception as e:
                logger.error(f"下载失败 {url}: {e}")
                return None


# 便捷异步函数
async def download_image_to_pil(url: str, timeout: int = 10) -> Optional[Image.Image]:
    """异步快速下载图片到PIL Image"""
    downloader = AsyncImageDownloader(timeout=timeout)
    return await downloader.download_to_pil(url)