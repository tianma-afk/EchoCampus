package com.echocampus.pojo;

import java.util.List;

public record LandmarkDetail(
        String id,
        String name,
        Integer score,
        List<String> images,
        Integer checkInCount,
        List<String> tags,
        String openTime,
        String categoryName,
        String builtTime,
        String description,
        String locationDescription,
        String campusName,
        String universityName,
        List<FloorInfo> floors
) {
}
