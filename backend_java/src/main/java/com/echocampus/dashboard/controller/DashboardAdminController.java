package com.echocampus.dashboard.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.echocampus.algorithm.mapper.TaskMapper;
import com.echocampus.category.entity.CategoryEntity;
import com.echocampus.category.mapper.CategoryMapper;
import com.echocampus.dashboard.vo.DashboardVO;
import com.echocampus.feedback.entity.FeedbackEntity;
import com.echocampus.feedback.mapper.FeedbackMapper;
import com.echocampus.landmark.entity.LandmarkEntity;
import com.echocampus.landmark.mapper.LandmarkMapper;
import com.echocampus.shared.annotation.RequireRole;
import com.echocampus.shared.vo.Result;
import com.echocampus.university.mapper.UniversityMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/dashboard")
@RequireRole
@Tag(name = "管理端仪表盘", description = "提供仪表盘统计数据")
public class DashboardAdminController {
    private final LandmarkMapper landmarkMapper;
    private final UniversityMapper universityMapper;
    private final FeedbackMapper feedbackMapper;
    private final TaskMapper taskMapper;
    private final CategoryMapper categoryMapper;

    public DashboardAdminController(LandmarkMapper landmarkMapper,
                                    UniversityMapper universityMapper,
                                    FeedbackMapper feedbackMapper,
                                    TaskMapper taskMapper,
                                    CategoryMapper categoryMapper) {
        this.landmarkMapper = landmarkMapper;
        this.universityMapper = universityMapper;
        this.feedbackMapper = feedbackMapper;
        this.taskMapper = taskMapper;
        this.categoryMapper = categoryMapper;
    }

    @GetMapping("/")
    @Operation(summary = "仪表盘统计", description = "返回地标数、大学数、待处理反馈数、任务数及分类分布")
    public Result<DashboardVO> getDashboard() {
        long landmarkCount = landmarkMapper.selectCount(null);
        long universityCount = universityMapper.selectCount(null);
        long pendingFeedbackCount = feedbackMapper.selectCount(
                new LambdaQueryWrapper<FeedbackEntity>()
                        .eq(FeedbackEntity::getStatus, "PENDING"));
        long taskCount = taskMapper.selectCount(null);

        List<CategoryEntity> categories = categoryMapper.selectList(null);
        List<DashboardVO.CategoryStat> categoryBreakdown = categories.stream()
                .map(cat -> {
                    long count = landmarkMapper.selectCount(
                            new LambdaQueryWrapper<LandmarkEntity>()
                                    .eq(LandmarkEntity::getCategoryId, cat.getId()));
                    return DashboardVO.CategoryStat.builder()
                            .categoryName(cat.getName())
                            .count(count)
                            .build();
                })
                .filter(stat -> stat.getCount() > 0)
                .toList();

        DashboardVO vo = DashboardVO.builder()
                .landmarkCount(landmarkCount)
                .universityCount(universityCount)
                .pendingFeedbackCount(pendingFeedbackCount)
                .taskCount(taskCount)
                .categoryBreakdown(categoryBreakdown)
                .build();
        return Result.success(vo);
    }
}
