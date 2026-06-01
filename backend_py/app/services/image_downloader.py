# services/image_downloader.py
import httpx
from io import BytesIO
from PIL import Image
from typing import Optional, Union
import asyncio

class AsyncImageDownloader:
    """异步图片下载服务"""
    
    def __init__(self, timeout: int = 10, max_size: Optional[int] = None):
        self.timeout = timeout
        self.max_size = max_size
    
    async def download_to_pil(self, url: str) -> Optional[Image.Image]:
        """异步下载并转换为RGB模式"""
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
                print(f"下载失败 {url}: {e}")
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
                print(f"下载失败 {url}: {e}")
                return None


# 便捷异步函数
async def download_image_to_pil(url: str, timeout: int = 10) -> Optional[Image.Image]:
    """异步快速下载图片到PIL Image"""
    downloader = AsyncImageDownloader(timeout=timeout)
    return await downloader.download_to_pil(url)