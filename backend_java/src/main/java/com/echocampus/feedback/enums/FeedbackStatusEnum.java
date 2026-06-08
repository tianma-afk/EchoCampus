package com.echocampus.feedback.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "反馈状态")
public enum FeedbackStatusEnum {
    @Schema(description = "待处理")
    PENDING("PENDING", "待处理"),

    @Schema(description = "已解决")
    RESOLVED("RESOLVED", "已解决"),

    @Schema(description = "已驳回")
    REJECTED("REJECTED", "已驳回");

    @EnumValue
    @JsonValue
    private final String value;

    private final String desc;

    public static FeedbackStatusEnum fromValue(String value) {
        for (FeedbackStatusEnum type : values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        return null;
    }
}
