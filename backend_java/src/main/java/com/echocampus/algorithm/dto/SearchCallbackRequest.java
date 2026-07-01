package com.echocampus.algorithm.dto;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 图像搜索任务回调请求 DTO
 * 用于接收 Python 算法服务返回的搜索结果
 */
@Data
@Schema(description = "图像搜索任务回调请求")
public class SearchCallbackRequest {

    /**
     * 任务结果状态
     * 可选值：SUCCESS, FAILED
     */
    @Schema(description = "任务结果状态", example = "SUCCESS", allowableValues = {"SUCCESS", "FAILED"})
    @NotBlank(message = "任务结果不能为空")
    private String result;

    /**
     * 匹配结果列表（仅在 result 为 SUCCESS 时存在）
     */
    @Schema(description = "匹配结果列表")
    @Valid
    private List<MatchResult> matches;

    /**
     * 单个匹配结果
     */
    @Data
    @Schema(description = "单个匹配结果")
    public static class MatchResult {

        /**
         * 匹配的图片 ID（UUID 格式）
         */
        @Schema(description = "匹配的图片 ID", example = "550e8400-e29b-41d4-a716-446655440000")
        @NotBlank(message = "图片 ID 不能为空")
        @JsonProperty("uuid")  // ← 告诉 Jackson：JSON 中的 "uuid" 映射到这个字段
        private String imageId;

        /**
         * 相似度分数（0-1 之间）
         */
        @Schema(description = "相似度分数", example = "0.95")
        @NotNull(message = "相似度分数不能为空")
        private Double score;

        /**
         * 二阶相似度分数（Pair-VPR pairwise similarity）
         */
        @Schema(description = "二阶相似度分数", example = "0.88")
        @JsonProperty("pair_similarity")
        private Double pairSimilarity;
    }
}
