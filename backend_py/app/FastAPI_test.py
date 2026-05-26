import requests
import core.select_pic as select_pic

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
