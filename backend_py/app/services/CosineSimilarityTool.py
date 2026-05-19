import numpy as np
from typing import List, Union, Optional
import math

"""
向量余弦相似度计算工具
返回值范围: 0 ~ 1 (经过归一化处理)
"""
    

def normalize_to_0_1(cosine_sim: float) -> float:
    """
    将原始余弦相似度 (-1~1) 归一化到 0~1 范围
    
       Args:
        cosine_sim: 原始余弦相似度，范围 [-1, 1]
            
    Returns:
        归一化后的相似度，范围 [0, 1]
    """
        # 公式: (x + 1) / 2
    return (cosine_sim + 1.0) / 2.0
    

def cosine_similarity_raw(vec1: Union[List[float], np.ndarray], 
                            vec2: Union[List[float], np.ndarray]) -> float:
    """
    计算原始余弦相似度（范围 -1~1）
    
    Args:
        vec1: 第一个向量
        vec2: 第二个向量
        
    Returns:
        原始余弦相似度，范围 [-1, 1]
        
    Raises:
        ValueError: 如果向量为空或维度不匹配
    """
    # 转换为 numpy 数组
    v1 = np.array(vec1, dtype=np.float64)
    v2 = np.array(vec2, dtype=np.float64)
    
    # 检查维度
    if v1.shape != v2.shape:
        raise ValueError(f"向量维度不匹配: {v1.shape} vs {v2.shape}")
    
    if len(v1) == 0:
        raise ValueError("向量不能为空")
    
    # 计算点积
    dot_product = np.dot(v1, v2)
    
    # 计算模长
    norm_v1 = np.linalg.norm(v1)
    norm_v2 = np.linalg.norm(v2)
    
    # 避免除零错误
    if norm_v1 == 0 or norm_v2 == 0:
        return 0.0
    
    # 计算余弦相似度
    raw_sim = dot_product / (norm_v1 * norm_v2)
    
    # 处理浮点误差导致的超出范围
    return np.clip(raw_sim, -1.0, 1.0)


def cosine_similarity_normalized(vec1: Union[List[float], np.ndarray],
                                    vec2: Union[List[float], np.ndarray]) -> float:
    """
    计算归一化的余弦相似度（范围 0~1）
    
    Args:
        vec1: 第一个向量
        vec2: 第二个向量
        
    Returns:
        归一化后的余弦相似度，范围 [0, 1]
    """
    raw_sim = cosine_similarity_raw(vec1, vec2)
    return normalize_to_0_1(raw_sim)


def batch_similarity(vectors: List[Union[List[float], np.ndarray]],
                        target: Union[List[float], np.ndarray]) -> List[float]:
    """
    批量计算多个向量与目标向量的归一化余弦相似度
    
    Args:
        vectors: 向量列表
        target: 目标向量
        
    Returns:
        归一化相似度列表，范围 [0, 1]
    """
    similarities = []
    for vec in vectors:
        sim = cosine_similarity_normalized(vec, target)
        similarities.append(sim)
    return similarities


def similarity_matrix(vectors: List[Union[List[float], np.ndarray]]) -> np.ndarray:
    """
    计算向量列表的相似度矩阵
    
    Args:
        vectors: 向量列表
        
    Returns:
        相似度矩阵 (n x n)，范围 [0, 1]
    """
    n = len(vectors)
    matrix = np.zeros((n, n))
    
    for i in range(n):
        for j in range(i, n):
            sim = cosine_similarity_normalized(vectors[i], vectors[j])
            matrix[i, j] = sim
            matrix[j, i] = sim
    
    return matrix


def find_most_similar(query: Union[List[float], np.ndarray],
                        candidates: List[Union[List[float], np.ndarray]],
                        top_k: int = 1) -> List[tuple]:
    """
    找到最相似的 top_k 个候选向量
    
    Args:
        query: 查询向量
        candidates: 候选向量列表
        top_k: 返回前 k 个最相似的
        
    Returns:
        [(相似度, 索引, 向量), ...] 按相似度降序排列
    """
    similarities = batch_similarity(candidates, query)
    
    # 获取 top_k 索引
    indices = np.argsort(similarities)[::-1][:top_k]
    
    results = []
    for idx in indices:
        results.append((similarities[idx], idx, candidates[idx]))
    
    return results


# ============ 便捷函数（最简单使用） ============

def cosine_sim(vec1, vec2, normalized=True):
    """
    最简单的余弦相似度计算函数
    
    Args:
        vec1: 第一个向量
        vec2: 第二个向量
        normalized: 是否归一化到 0~1（默认 True）
        
    Returns:
        余弦相似度
    """
    if normalized:
        return cosine_similarity_normalized(vec1, vec2)
    else:
        return cosine_similarity_raw(vec1, vec2)


# ============ 测试代码 ============
"""测试 CosineSimilarityTool 的功能和正确性
if __name__ == "__main__":
    # 测试向量
    vec_a = [1, 0, 0]
    vec_b = [1, 0, 0]      # 完全相同
    vec_c = [0, 1, 0]      # 垂直
    vec_d = [-1, 0, 0]     # 完全相反
    vec_e = [0.5, 0.5, 0]  # 中间状态
    
    print("=" * 50)
    print("余弦相似度测试（0~1范围）")
    print("=" * 50)
    
    # 测试各种情况
    sim1 = cosine_sim(vec_a, vec_b)
    print(f"完全相同 [1,0,0] vs [1,0,0]: {sim1:.4f} (期望: 1.0)")
    
    sim2 = cosine_sim(vec_a, vec_c)
    print(f"垂直 [1,0,0] vs [0,1,0]:   {sim2:.4f} (期望: 0.5)")
    
    sim3 = cosine_sim(vec_a, vec_d)
    print(f"完全相反 [1,0,0] vs [-1,0,0]: {sim3:.4f} (期望: 0.0)")
    
    sim4 = cosine_sim(vec_a, vec_e)
    print(f"中间 [1,0,0] vs [0.5,0.5,0]: {sim4:.4f}")
    
    print("\n" + "=" * 50)
    print("批量计算示例")
    print("=" * 50)
    
    # 批量计算
    query = [1, 0, 0]
    candidates = [[1, 0.1, 0], [0.5, 0.5, 0], [0, 1, 0], [-0.5, 0, 0]]
    
    results = CosineSimilarityTool.find_most_similar(query, candidates, top_k=3)
    
    print(f"查询向量: {query}")
    print("Top-3 最相似的:")
    for sim, idx, vec in results:
        print(f"  [{idx}] 相似度: {sim:.4f}, 向量: {vec}")
    
    print("\n" + "=" * 50)
    print("相似度矩阵")
    print("=" * 50)
    
    vectors = [[1, 0], [0.5, 0.5], [0, 1], [-0.5, 0.5]]
    matrix = CosineSimilarityTool.similarity_matrix(vectors)
    print(matrix)
    
    # CosPlace/EigenPlaces 512维向量使用示例
    print("\n" + "=" * 50)
    print("512维向量示例（模拟CosPlace/EigenPlaces输出）")
    print("=" * 50)
    
    # 模拟两个 512 维的特征向量
    np.random.seed(42)
    feature1 = np.random.randn(512)  # CosPlace/EigenPlaces 描述子
    feature2 = np.random.randn(512)
    
    sim = cosine_sim(feature1, feature2)
    print(f"随机512维向量相似度: {sim:.6f}")
    
    # 相同向量的相似度
    sim_same = cosine_sim(feature1, feature1)
    print(f"同一向量相似度: {sim_same:.6f}")
"""