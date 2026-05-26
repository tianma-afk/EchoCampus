package com.echocampus.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LandmarkDetailVO {
    private UUID id;
    private String name;
    private BigDecimal rating;
    private Integer checkins;
    private String openTime;
    private String category;
    private UUID categoryId;
    private List<String> tags;
    private List<String> imgs;
    private String buildYear;
    private String openTimeDetail;
    private String floors;
    private String location;
    private String description;
    private String campusName;
    private UUID campusId;
    private String universityName;
    private UUID universityId;
    private Integer totalFloors;
    private BigDecimal recommendRate;
    private List<FloorVO> floorList;
}
