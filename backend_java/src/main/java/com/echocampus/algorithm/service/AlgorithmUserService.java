package com.echocampus.algorithm.service;

import com.echocampus.algorithm.dto.SearchCallbackRequest;
import com.echocampus.algorithm.vo.SearchResultVO;

import java.util.List;
import java.util.UUID;

/**
 * 移动端算法服务接口
 * 提供图像搜索等算法相关功能
 */
public interface AlgorithmUserService {

    /**
     * 创建图像搜索任务
     * 
     * 功能说明：
     * 1. 获取移动端用户上传的图片 URL
     * 2. 创建 search 类型的任务记录到数据库
     * 3. 构造回调 URL
     * 4. 发送任务给 Python 算法服务（暂不实现真正通信）
     * 5. 获得算法任务 ID，更新任务数据库表
     * 
     * @param imageUrl 移动端用户上传到 MinIO 的图片 URL
     *                 格式示例：http://localhost:9000/campus/images/2025/05/30/a1b2c3d4.jpg
     * @return 任务 ID（Java 端生成的 UUID）
     */
    UUID createSearchTask(String imageUrl);

    /**
     * 更新图像搜索任务状态
     * 
     * 功能说明：
     * 1. 根据 taskId 查询任务记录
     * 2. 更新任务状态为 Python 返回的结果（SUCCESS/FAILED）
     * 3. 如果任务成功，处理匹配结果：imageId → landmarkId → coverImageId → coverUrl
     * 4. 按 landmark 分组取最高相似度，序列化存入 task.searchResult
     * 
     * @param taskId 任务 ID
     * @param request Python 回调请求，包含任务状态和匹配结果
     */
    void updateSearchTaskStatus(UUID taskId, SearchCallbackRequest request);

    /**
     * 查询图像搜索结果
     * 
     * 功能说明：
     * 1. 根据 taskId 查询任务记录的 searchResult 字段
     * 2. 反序列化 JSON 为 List<SearchResultVO>
     * 3. 返回给前端展示识别结果
     * 
     * @param taskId 任务 ID
     * @return 识别结果列表，每条包含建筑名、最高相似度、封面图 URL
     */
    List<SearchResultVO> getSearchResult(UUID taskId);
}
