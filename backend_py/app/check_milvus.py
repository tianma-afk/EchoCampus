import sys
from pathlib import Path
sys.path.insert(0, str(Path(__file__).resolve().parent))
from core import milvus_service

milvus_service.milvus_init()
count = milvus_service.service.get_collection_count()
print(f"Milvus collection count: {count}")

# 濡傛灉鏈夋暟鎹? 鍒楀嚭 UUID
if count > 0:
    from pymilvus import MilvusClient
    client = MilvusClient(uri="http://localhost:19530")
    results = client.query(
        collection_name="image_collection",
        filter="uuid != ''",
        output_fields=["uuid"],
        limit=10
    )
    for r in results:
        print(f"  UUID: {r['uuid']}")
