package com.echocampus.pojo;

import java.util.List;

public record FloorInfo(
        Integer floorNumber,
        String floorName,
        List<String> tags
) {
}
