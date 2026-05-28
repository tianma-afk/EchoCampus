package com.echocampus.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
public class LandmarkUpdateRequest {

    @Schema(description = "地标名称")
    private String name;

    @Schema(description = "评分，0-5")
    private BigDecimal rating;

    @Schema(description = "打卡人数")
    private Integer checkInCount;

    @Schema(description = "开放时间")
    private String openTime;

    @Schema(description = "分类ID")
    private UUID categoryId;

    @Schema(description = "标签列表")
    private List<String> tags;

    @Schema(description = "建成年份")
    private String buildYear;

    @Schema(description = "开放时间详情")
    private String openTimeDetail;

    @Schema(description = "楼层范围")
    private String floors;

    @Schema(description = "位置描述")
    private String location;

    @Schema(description = "地标描述")
    private String description;

    @Schema(description = "所属校区ID")
    private UUID campusId;

    @Schema(description = "总楼层数")
    private Integer totalFloors;

    @Schema(description = "推荐率")
    private BigDecimal recommendRate;

    @Schema(description = "楼层列表（传入则全量替换）")
    private List<FloorCreateDTO> floorList;
}
