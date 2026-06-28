package com.echocampus.landmark.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LandmarkAdminVO {
    private UUID id;
    private String name;
    private String categoryName;
    private UUID categoryId;
    private String universityName;
    private String campusName;
    private UUID campusId;
    private String coverImageUrl;
    private BigDecimal rating;
    private Integer checkInCount;
    private BigDecimal recommendRate;
    private Integer favoriteCount;
    private String buildYear;
    private String openTime;
}
