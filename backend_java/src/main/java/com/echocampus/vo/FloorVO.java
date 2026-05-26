package com.echocampus.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FloorVO {
    private java.util.UUID id;
    private Integer floorNumber;
    private String floorName;
    private List<String> tags;
}
