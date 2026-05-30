package com.echocampus.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "任务状态")
public enum TaskStatusEnum {
    @Schema(description = "任务就绪")
    READY("READY", "就绪"),

    @Schema(description = "任务处理中")
    PROCESSING("PROCESSING", "处理中"),

    @Schema(description = "任务成功")
    SUCCESS("SUCCESS", "成功"),

    @Schema(description = "任务失败")
    FAILED("FAILED", "失败");


    @EnumValue
    @JsonValue
    private final String value;

    private final String desc;

    public static TaskStatusEnum fromValue(String value) {
        for (TaskStatusEnum type : values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        return null;
    }
}
