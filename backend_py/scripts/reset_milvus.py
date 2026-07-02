"""清空 Milvus image_collection 并重建"""
from pathlib import Path
import sys

sys.path.append(str(Path(__file__).resolve().parents[1] / "app"))

from core import milvus_service

milvus_service.milvus_init()
print("正在清空 Milvus 集合...")
milvus_service.service.reset_collection()
print("Milvus 集合已清空并重建完成")
