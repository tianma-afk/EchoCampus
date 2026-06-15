package com.echocampus.user.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@TableName("recognition_record")
public class RecognitionRecord {

    @TableId
    private UUID id;

    private UUID userId;

    private UUID imageId;

    private UUID taskId;

    private UUID landmarkId;

    private Double similarity;

    private OffsetDateTime createdAt;
}
