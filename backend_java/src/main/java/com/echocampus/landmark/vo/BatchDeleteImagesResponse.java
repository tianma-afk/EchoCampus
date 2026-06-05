package com.echocampus.landmark.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchDeleteImagesResponse {
    private int deletedCount;
    private boolean affectedCover;
    private int affectedCuratedCount;
}
