package com.echocampus.feedback.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "反馈类型")
public enum FeedbackTypeEnum {
    @Schema(description = "信息错误")
    INFO_ERROR("INFO_ERROR", "信息错误"),

    @Schema(description = "违禁内容")
    CONTENT_ILLEGAL("CONTENT_ILLEGAL", "违禁内容"),

    @Schema(description = "信息变更")
    INFO_CHANGE("INFO_CHANGE", "信息变更"),

    @Schema(description = "其他")
    OTHER("OTHER", "其他");

    @EnumValue
    @JsonValue
    private final String value;

    private final String desc;

    public static FeedbackTypeEnum fromValue(String value) {
        for (FeedbackTypeEnum type : values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        return null;
    }
}
