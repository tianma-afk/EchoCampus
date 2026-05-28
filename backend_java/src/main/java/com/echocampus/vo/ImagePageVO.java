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
public class ImagePageVO {
    private List<LandmarkImageVO> records;
    private long total;
    private boolean hasMore;
}
