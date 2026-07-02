package com.echocampus.shared.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "任务分类")
public enum TaskTypeEnum {
    @Schema(description = "向量化任务")
    VECTORIZE("VECTORIZE", "向量化任务"),

    @Schema(description = "图搜任务")
    SEARCH("SEARCH", "图搜任务"),

    @Schema(description = "向量删除任务")
    DELETE("DELETE", "向量删除任务");

    @EnumValue
    @JsonValue
    private final String value;

    private final String desc;
    
    public static TaskTypeEnum fromValue(String value) {
        for (TaskTypeEnum type : values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        return null;
    }
}