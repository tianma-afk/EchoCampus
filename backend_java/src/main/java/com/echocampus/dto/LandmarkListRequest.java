package com.echocampus.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class LandmarkListRequest {

    @Schema(description = "当前页码，从1开始", example = "1")
    @Min(value = 1, message = "页码不能小于1")
    private Integer page = 1;

    @Schema(description = "每页条数", example = "10")
    @Min(value = 1, message = "每页条数不能小于1")
    @Max(value = 100, message = "每页条数不能大于100")
    private Integer pageSize = 6;

    @Schema(description = "分类筛选，null表示不限分类，1-6表示筛选特定分类",
            allowableValues = {"1", "2", "3", "4", "5", "6", "null"},
            example = "5")
    @Min(value = 1, message = "分类必须在1-6之间")
    @Max(value = 6, message = "分类必须在1-6之间")
    private Integer category;

    @Schema(description = "搜索关键词，用于模糊匹配景点名称或描述",
            example = "篮球场")
    private String searchQuery;

    @Schema(description = "排序规则，例如默认排序：'default';按评分排序：'rate'；按热度排序：'hot'；不填时为默认排序",
            allowableValues = {"default", "rate", "hot", "null"},
            example = "default")
    private String sortBy;
}
