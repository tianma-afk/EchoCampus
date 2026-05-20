from pymilvus import MilvusClient
from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parents[3]  # 往上3级
DATA_DIR = PROJECT_ROOT / "data"
MILVUS_DB_PATH = DATA_DIR / "milvus_lite.db"

COLLECTION_NAME = "image_collection"
VECTOR_DIM = 4096
METRIC_TYPE = "IP"

# 索引参数（HNSW 算法，在速度和准确率之间取得很好的平衡）
INDEX_PARAMS = {
    "metric_type": METRIC_TYPE,
    "index_type": "HNSW",
    "params": {
        "M": 32,              # 每个节点的最大连接数，越大越准但内存占用越高（建议 16~64）
        "efConstruction": 200 # 构建索引时的搜索宽度，越大索引质量越高但构建越慢
    }
}

# 搜索参数（查询时可以动态调整 ef 来平衡速度和精度）
SEARCH_PARAMS = {
    "metric_type": METRIC_TYPE,
    "params": {"ef": 128}     # 搜索时的动态列表大小，越大召回率越高（建议 64~256）
}

DATA_DIR.mkdir(parents=True, exist_ok=True)
client = MilvusClient(str(MILVUS_DB_PATH))
print("✅MilvusLite连接成功")

if not client.has_collection(COLLECTION_NAME):
    client.create_collection(
        collection_name=COLLECTION_NAME,
        dimension=VECTOR_DIM,
        auto_id=False,
        metric_type=METRIC_TYPE
    )
    print(f"库 {COLLECTION_NAME} 创建成功")
else:
    print(f"库 {COLLECTION_NAME} 已存在")

indexes = client.list_indexes(collection_name=COLLECTION_NAME)
if "vector" not in indexes:
    client.create_index(
        collection_name=COLLECTION_NAME,
        field_name="vector",
        index_params=INDEX_PARAMS
    )
    print("✅ 索引创建成功")
else:
    print("✅ 索引已存在，跳过创建")

def insert_vector(vector, id=None):
    if id is None:
        print("错误: id不能为None")
        return None
    
    data = [{"id": id, "vector": vector}]

    result = client.insert(
        collection_name=COLLECTION_NAME,
        data=data
    )
    client.load_collection(collection_name=COLLECTION_NAME)
    return result
def insert_vectors(vectors, ids=None):
    data_to_insert = []
    if vectors is None or len(vectors) == 0:
        print("错误: vectors不能为None或空")
        return None
    
    if ids is None or len(ids) != len(vectors):
        print("错误: ids不能为None或长度与vectors不一致")
        return None
    
    for i, vector in enumerate(vectors):
        data_to_insert.append({"id": ids[i], "vector": vector})
    result = client.insert(
        collection_name=COLLECTION_NAME,
        data=data_to_insert
    )
    client.load_collection(collection_name=COLLECTION_NAME)
    return result

def search_similar(query_vector, top_k=10):
    results = client.search(
        collection_name=COLLECTION_NAME,  # 集合名称
        data=[query_vector],              # 查询的向量（支持批量，这里用单条）
        limit=top_k,                      # 返回数量
        search_params=SEARCH_PARAMS,      # 搜索参数（你已定义好）
        output_fields=["id"]              # 需要返回的字段（这里只要id）
    )
    
    return results[0]



