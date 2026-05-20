package com.echocampus.pojo;

import java.util.List;

public record PageResponse<T>(
        long total,
        int pageNum,
        int pageSize,
        int pages,
        List<T> list
) {
}
