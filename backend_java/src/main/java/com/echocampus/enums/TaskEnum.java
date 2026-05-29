package com.echocampus.enums;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "任务分类")
public enum TaskEnum {
    @Schema(description = "向量化任务")
    VECTORIZE("vectorize"),

    @Schema(description = "搜索任务")
    SEARCH("search");

    private String value;
    public String getValue() { return value; }
}