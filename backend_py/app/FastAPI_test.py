import requests
import core.select_pic as select_pic


def search_test():
    goal_path = select_pic.select_single_file()

    url = 'http://127.0.0.1:8000/search'
    data = {
        "pic_path": goal_path,
        "top_k": 10
    }
    # 使用 requests.post，并用 json 参数传递数据
    response = requests.post(url, json=data) 


    # 检查是否请求成功
    if response.status_code == 200:
        result = response.json()
        print(f"📢 接口消息: {result.get('message')}")

        
        # 遍历 results 数组，只打印我们最关心的字段
        print("搜索结果:")
        for index, item in enumerate(result.get('results_1', []), 1):
            print(f"  - {item['filename']} (ID: {item['uuid']} , score: {item['score']})")
        print("搜索结果（按相似度排序）:")    
        for index, item in enumerate(result.get('results_2', []), 1):
            print(f"  - {item['filename']} (ID: {item['uuid']} , score: {item['score']})")
    else:
        # 即使请求失败，也尝试打印服务器返回的错误详情
        print(f"❌ 请求异常 [{response.status_code}]: {response.text}")


def insert_test_one():
    goal_path = select_pic.select_single_file()
    url = 'http://127.0.0.1:8000/insert_one'
    data = {
        "pic_url": goal_path
    }
    response = requests.post(url, json=data) 
    
    if response.status_code == 200:
        result = response.json()
        print(f"📢 插入消息: {result.get('message')}")
    else:
        print(f"❌ 插入异常 [{response.status_code}]: {response.text}") 
        
def insert_test():
    url = "http://127.0.0.1:8000/insert" 
    # 1. 获取用户选择的多个图片路径
    goal_paths = ["D:/EchoCampus/backend_py/app/temp_resources/C2.jpg", "D:/EchoCampus/backend_py/app/temp_resources/C3.jpg"]
    if not goal_paths:
        print("未选择任何图片")
        return

    # 2. 构建 items 列表
    items = []
    for pic_path in goal_paths:
        # filename = os.path.basename(pic_path)
        # stable_id = hashlib.md5(filename.encode("utf-8")).hexdigest() + "----"
        items.append({
            # "uuid": stable_id,
            "pic_url": pic_path
        })

    # 3. 构造请求体（符合后端期望的格式）
    request_body = {"items": items}
    
    response = requests.post(url, json=request_body)
    if response.status_code == 200:
        result = response.json()
        print(f"📢 插入消息: {result.get('message')}")
    else:
        print(f"❌ 插入异常 [{response.status_code}]: {response.text}")    
        
             
        
if __name__ == "__main__":
    insert_test()
    search_test() 
    
