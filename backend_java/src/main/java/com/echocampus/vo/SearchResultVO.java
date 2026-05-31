package com.echocampus.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SearchResultVO {
    private String landmarkId;
    private String landmarkName;
    private Double similarity;
    private String coverUrl;
}
