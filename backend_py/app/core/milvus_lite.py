from pymilvus import MilvusClient,CollectionSchema, FieldSchema, DataType
from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parents[3]  # 往上3级
DATA_DIR = PROJECT_ROOT / "data"
MILVUS_DB_PATH = DATA_DIR / "milvus_lite.db"

COLLECTION_NAME = "image_collection"
VECTOR_DIM = 512
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
    # 定义 schema，明确 id 字段类型为 VARCHAR (字符串)
    schema = CollectionSchema([
        FieldSchema(name="id", dtype=DataType.VARCHAR, max_length=36, is_primary=True),
        FieldSchema(name="vector", dtype=DataType.FLOAT_VECTOR, dim=VECTOR_DIM),
        FieldSchema(name="filename", dtype=DataType.VARCHAR, max_length=256)
    ])
    
    client.create_collection(
        collection_name=COLLECTION_NAME,
        schema=schema  # 使用自定义 schema 而不是简化版创建
    )
    print(f"库 {COLLECTION_NAME} 创建成功")
else:
    print(f"库 {COLLECTION_NAME} 已存在")

indexes = client.list_indexes(collection_name=COLLECTION_NAME)
if "vector" not in indexes:
    # 使用 MilvusClient.prepare_index_params() 方法创建索引参数
    index_params = client.prepare_index_params()
    index_params.add_index(
        field_name="vector",
        index_type=INDEX_PARAMS["index_type"],
        metric_type=INDEX_PARAMS["metric_type"],
        params=INDEX_PARAMS["params"]
    )
    
    client.create_index(
        collection_name=COLLECTION_NAME,
        index_params=index_params
    )
    print("✅ 索引创建成功")
else:
    print("✅ 索引已存在，跳过创建")

def insert_vector(vector, id: str = None, filename: str = None):  # 改为 str 类型
    if id is None:
        print("错误: id不能为None")
        return None
    
    # 可选：验证 id 格式
    if len(id) != 36:
        print(f"警告: id '{id}' 长度不是36位")
    
    data = [{"id": id, "vector": vector, "filename": filename}]

    result = client.insert(
        collection_name=COLLECTION_NAME,
        data=data
    )
    client.load_collection(collection_name=COLLECTION_NAME)
    return result
def insert_vectors(vectors, ids: list = None,filenames : list = None):  # ids 改为字符串列表
    data_to_insert = []
    if vectors is None or len(vectors) == 0:
        print("错误: vectors不能为None或空")
        return None
    
    if ids is None or len(ids) != len(vectors):
        print("错误: ids不能为None或长度与vectors不一致")
        return None
    
    if filenames is None or len(filenames) != len(vectors):
        print("错误: filenames不能为None或长度与vectors不一致")
        return None
    
    # 可选：验证每个 id 长度
    for id_val in ids:
        if len(id_val) != 36:
            print(f"警告: id '{id_val}' 长度不是36位")
    
    for i, vector in enumerate(vectors):
        data_to_insert.append({"id": ids[i], "vector": vector, "filename": filenames[i]})
    result = client.insert(
        collection_name=COLLECTION_NAME,
        data=data_to_insert
    )
    client.load_collection(collection_name=COLLECTION_NAME)
    return result

def load_collection():
    client.load_collection(collection_name=COLLECTION_NAME)

def search_similar(query_vector, top_k=10):
    results = client.search(
        collection_name=COLLECTION_NAME,  # 集合名称
        data=[query_vector],              # 查询的向量（支持批量，这里用单条）
        limit=top_k,                      # 返回数量
        search_params=SEARCH_PARAMS,      # 搜索参数（你已定义好）
        output_fields=["id", "filename"]  # 需要返回的字段（这里只要id和filename）
    )
    
    return [{'id': r['id'], 'filename': r['filename'], 'score': r['distance']} for r in results[0]]



