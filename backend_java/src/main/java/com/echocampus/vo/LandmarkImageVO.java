package com.echocampus.vo;

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
public class LandmarkImageVO {
    private UUID id;
    private String url;
    private boolean isCover;

    private boolean isCurated;
    private String fileExt;
    private OffsetDateTime createdAt;
}
