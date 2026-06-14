package com.echocampus.shared.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "角色分类")
public enum RoleEnum {

    @Schema(description = "用户")
    USER("USER", "用户", 0),

    @Schema(description = "管理员")
    ADMIN("ADMIN", "管理员", 1),

    @Schema(description = "超级管理员")
    SUPER_ADMIN("SUPER_ADMIN", "超级管理员", 2);

    @EnumValue
    @JsonValue
    private final String value;

    private final String desc;
    private final int level;

    public static RoleEnum fromValue(String value) {
        for (RoleEnum type : values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        return null;
    }

}
