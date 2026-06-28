package com.echocampus.dashboard.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardVO {
    private long landmarkCount;
    private long universityCount;
    private long pendingFeedbackCount;
    private long taskCount;
    private List<CategoryStat> categoryBreakdown;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryStat {
        private String categoryName;
        private long count;
    }
}
