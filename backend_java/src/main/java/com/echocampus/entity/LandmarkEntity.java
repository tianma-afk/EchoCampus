package com.echocampus.entity;

import lombok.Data;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@TableName(value = "landmark", autoResultMap = true)
public class LandmarkEntity {

    @TableId
    private UUID id;

    private String name;

    private BigDecimal rating;

    private Integer checkInCount;

    private String openTime;

    private UUID categoryId;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> tags;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> imgs;

    private String coverImg;

    private String buildYear;

    private String openTimeDetail;

    private String floors;

    private String location;

    private String description;

    private UUID campusId;

    private Integer totalFloors;

    private BigDecimal recommendRate;
}
