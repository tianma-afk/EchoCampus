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
public class FeedbackUserVO {
    private UUID id;
    private UUID landmarkId;
    private String landmarkName;
    private String feedbackType;
    private String content;
    private String status;
    private String resolveNote;
    private OffsetDateTime resolveTime;
    private OffsetDateTime createdAt;
    private String uploadUrl;
    private String correctLandmarkName;
}
