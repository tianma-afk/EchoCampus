package com.echocampus.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.echocampus.handler.StringJsonbTypeHandler;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@TableName(value = "task", autoResultMap = true)
public class TaskEntity {

    @TableId
    private UUID id;

    private String taskType;

    private String algTaskId;

    private String taskStatus;

    @TableField(typeHandler = StringJsonbTypeHandler.class)
    private String searchResult;

    private OffsetDateTime createdAt;

    private OffsetDateTime updatedAt;
}
