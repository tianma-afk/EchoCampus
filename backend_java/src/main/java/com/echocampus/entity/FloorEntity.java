package com.echocampus.entity;

import lombok.Data;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.echocampus.handler.JsonbTypeHandler;

import java.util.List;
import java.util.UUID;

@Data
@TableName(value = "floor", autoResultMap = true)
public class FloorEntity {

    @TableId
    private UUID id;

    private UUID landmarkId;

    private Integer floorNumber;

    private String floorName;

    @TableField(typeHandler = JsonbTypeHandler.class)
    private List<String> tags;
}
