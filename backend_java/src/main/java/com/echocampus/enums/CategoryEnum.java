package com.echocampus.enums;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;


@Getter
@AllArgsConstructor
@Schema(description = "地标分类")
public enum CategoryEnum {

    @Schema(description = "教学楼")
    TEACHING_BUILDING(1, "教学楼"),

    @Schema(description = "图书馆")
    LIBRARY(2, "图书馆"),

    @Schema(description = "体育场馆")
    SPORTS_VENUE(3, "体育场馆"),

    @Schema(description = "生活区")
    LIVING_AREA(4, "生活区"),

    @Schema(description = "活动场馆")
    ACTIVITY_VENUE(5, "活动场馆"),

    @Schema(description = "景观景点")
    SCENIC_SPOT(6, "景观景点");

    /**
     * 前端传递的数字编码（1-6）
     */
    private final Integer code;

    /**
     * 数据库存储的中文名称
     */
    private final String dbName;

    /**
     * 根据前端数字编码获取枚举
     * @param code 前端传的数字（1-6）
     * @return 枚举对象，找不到返回 null
     */
    public static CategoryEnum fromCode(Integer code) {
        if (code == null) return null;
        return Arrays.stream(values())
                .filter(category -> category.code.equals(code))
                .findFirst()
                .orElse(null);
    }

    /**
     * 根据前端数字编码获取数据库中文名
     * @param code 前端传的数字（1-6）
     * @return 中文名称（如"教学楼"），找不到返回 null
     */
    public static String getDbNameByCode(Integer code) {
        CategoryEnum category = fromCode(code);
        return category != null ? category.dbName : null;
    }

    /**
     * 根据数据库中文名获取枚举
     * @param dbName 数据库存储的中文名
     * @return 枚举对象，找不到返回 null
     */
    public static CategoryEnum fromDbName(String dbName) {
        if (dbName == null || dbName.isEmpty()) return null;
        return Arrays.stream(values())
                .filter(category -> category.dbName.equals(dbName))
                .findFirst()
                .orElse(null);
    }

    /**
     * 根据数据库中文名获取前端数字编码
     * @param dbName 数据库存储的中文名
     * @return 数字编码（1-6），找不到返回 null
     */
    public static Integer getCodeByDbName(String dbName) {
        CategoryEnum category = fromDbName(dbName);
        return category != null ? category.code : null;
    }

    /**
     * 验证前端传的数字是否有效
     * @param code 前端传的数字
     * @return 是否有效
     */
    public static boolean isValidCode(Integer code) {
        return code != null && fromCode(code) != null;
    }
}