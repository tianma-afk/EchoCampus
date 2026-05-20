package com.echocampus.pojo;

import java.util.List;

public record LandmarkPageItem(
        String id,
        String name,
        Integer score,
        String coverUrl,
        Integer checkInCount,
        List<String> tags,
        String openTime,
        String categoryName
) {
}
