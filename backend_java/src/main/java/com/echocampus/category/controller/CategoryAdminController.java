package com.echocampus.category.controller;

import com.echocampus.category.entity.CategoryEntity;
import com.echocampus.category.mapper.CategoryMapper;
import com.echocampus.category.vo.CategoryVO;
import com.echocampus.shared.vo.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/admin/categories")
@Tag(name = "管理员分类", description = "提供分类的查询功能")
public class CategoryAdminController {
    private final CategoryMapper categoryMapper;

    public CategoryAdminController(CategoryMapper categoryMapper) {
        this.categoryMapper = categoryMapper;
    }

    @GetMapping("/")
    @Operation(summary = "获取所有分类", description = "返回所有地标分类")
    public Result<List<CategoryVO>> listCategories() {
        List<CategoryEntity> entities = categoryMapper.selectList(null);
        List<CategoryVO> vos = entities.stream()
                .map(e -> CategoryVO.builder()
                        .id(e.getId())
                        .name(e.getName())
                        .build())
                .collect(Collectors.toList());
        return Result.success(vos);
    }
}
