package com.echocampus.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@TableName("task")
public class TaskEntity {

    @TableId
    private UUID id;

    private String taskType;

    private String algTaskId;

    private String taskStatus;

    private OffsetDateTime createdAt;

    private OffsetDateTime updatedAt;
}
