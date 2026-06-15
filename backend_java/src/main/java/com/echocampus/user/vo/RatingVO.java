package com.echocampus.user.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RatingVO {

    private UUID id;

    private UUID landmarkId;

    private String landmarkName;

    private String coverImageUrl;

    private BigDecimal landmarkRating;

    private String category;

    private BigDecimal rating;

    private OffsetDateTime createdAt;

    private OffsetDateTime updatedAt;
}
