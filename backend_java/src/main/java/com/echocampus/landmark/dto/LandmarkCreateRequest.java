package com.echocampus.landmark.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
public class LandmarkCreateRequest {

    @Schema(description = "地标名称", example = "图书馆")
    @NotBlank(message = "地标名称不能为空")
    @Size(max = 200, message = "地标名称不能超过200个字符")
    private String name;

    @Schema(description = "评分，0-5", example = "4.5")
    private BigDecimal rating;

    @Schema(description = "打卡人数", example = "0")
    private Integer checkInCount;

    @Schema(description = "开放时间", example = "08:00-22:00")
    @Size(max = 50, message = "开放时间不能超过50个字符")
    private String openTime;

    @Schema(description = "分类ID", example = "550e8400-e29b-41d4-a716-446655440000")
    @NotNull(message = "分类ID不能为空")
    private UUID categoryId;

    @Schema(description = "标签列表", example = "[\"自习\", \"安静\"]")
    private List<String> tags;

    @Schema(description = "建成年份", example = "2010")
    @Size(max = 30, message = "建成年份不能超过30个字符")
    private String buildYear;

    @Schema(description = "开放时间详情", example = "周一至周日 08:00-22:00")
    @Size(max = 200, message = "开放时间详情不能超过200个字符")
    private String openTimeDetail;

    @Schema(description = "楼层范围", example = "1-5")
    @Size(max = 200, message = "楼层范围不能超过200个字符")
    private String floors;

    @Schema(description = "位置描述", example = "校园中心")
    @Size(max = 200, message = "位置描述不能超过200个字符")
    private String location;

    @Schema(description = "地标描述", example = "学校主图书馆，藏书丰富")
    private String description;

    @Schema(description = "所属校区ID", example = "550e8400-e29b-41d4-a716-446655440000")
    @NotNull(message = "校区ID不能为空")
    private UUID campusId;

    @Schema(description = "总楼层数", example = "5")
    private Integer totalFloors;

    @Schema(description = "推荐率", example = "0.95")
    private BigDecimal recommendRate;

    @Schema(description = "GPS 纬度", example = "39.9928000")
    private BigDecimal latitude;

    @Schema(description = "GPS 经度", example = "116.3280000")
    private BigDecimal longitude;

    @Schema(description = "楼层列表")
    private List<FloorCreateDTO> floorList;
}
