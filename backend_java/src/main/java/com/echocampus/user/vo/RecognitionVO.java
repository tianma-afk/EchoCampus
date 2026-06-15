package com.echocampus.user.vo;

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
public class RecognitionVO {

    private UUID id;

    private String imageUrl;

    private UUID landmarkId;

    private String landmarkName;

    private String coverImageUrl;

    private Double similarity;

    private OffsetDateTime createdAt;
}
