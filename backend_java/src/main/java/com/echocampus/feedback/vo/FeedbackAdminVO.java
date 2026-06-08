package com.echocampus.feedback.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackAdminVO {
    private UUID id;
    private UUID landmarkId;
    private String landmarkName;
    private UUID userId;
    private String feedbackType;
    private String content;
    private String status;
    private UUID adminId;
    private OffsetDateTime resolveTime;
    private String resolveNote;
    private OffsetDateTime createdAt;
}
