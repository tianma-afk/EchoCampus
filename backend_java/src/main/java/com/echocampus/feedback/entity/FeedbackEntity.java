package com.echocampus.feedback.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@TableName("feedback")
public class FeedbackEntity {

    @TableId
    private UUID id;

    private UUID landmarkId;

    private UUID userId;

    private String feedbackType;

    private String content;

    private String status;

    private UUID adminId;

    private OffsetDateTime resolveTime;

    private String resolveNote;

    private OffsetDateTime createdAt;

    private OffsetDateTime updatedAt;
}
