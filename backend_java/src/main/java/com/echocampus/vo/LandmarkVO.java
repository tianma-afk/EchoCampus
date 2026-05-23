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
public class LandmarkVO {
    private UUID id;
    private String name;
    private BigDecimal rating;
    private Integer checkins ;
    private String openTime;
    private String category;
    private List<String> tags;
    private String coverImg;



}