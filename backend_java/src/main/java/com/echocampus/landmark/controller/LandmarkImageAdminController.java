package com.echocampus.landmark.controller;

import com.echocampus.landmark.dto.BatchDeleteImagesRequest;
import com.echocampus.landmark.dto.CuratedImagesRequest;
import com.echocampus.landmark.dto.ImageConfirmRequest;
import com.echocampus.landmark.dto.ImagePresignRequest;
import com.echocampus.landmark.dto.ImagePresignResponse;
import com.echocampus.landmark.dto.SetCoverRequest;
import com.echocampus.landmark.service.LandmarkImageAdminService;
import com.echocampus.landmark.vo.BatchDeleteImagesResponse;
import com.echocampus.landmark.vo.ImagePageVO;
import com.echocampus.shared.vo.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/landmarks/{landmarkId}")
@Tag(name = "管理员地标图片", description = "提供地标图片的上传和管理功能")
public class LandmarkImageAdminController {
    private static final Logger log = LoggerFactory.getLogger(LandmarkImageAdminController.class);
    private final LandmarkImageAdminService landmarkImageAdminService;

    public LandmarkImageAdminController(LandmarkImageAdminService landmarkImageAdminService) {
        this.landmarkImageAdminService = landmarkImageAdminService;
    }

    @GetMapping("/images")
    @Operation(summary = "获取图片分页列表", description = "分页获取地标下图片，置顶封面和精选，含预签名URL")
    public Result<ImagePageVO> listImages(
            @PathVariable UUID landmarkId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        try {
            return Result.success(landmarkImageAdminService.listImages(landmarkId, page, pageSize));
        } catch (RuntimeException e) {
            log.error("listImages failed for landmark {}: {}", landmarkId, e.getMessage(), e);
            return Result.failure(404, e.getMessage());
        }
    }

    @PostMapping("/images/presign")
    @Operation(summary = "获取预签名上传 URL", description = "生成不可猜测的 key 和预签名 PUT URL，前端用该 URL 直传 MinIO")
    public Result<ImagePresignResponse> presignUpload(
            @PathVariable UUID landmarkId,
            @Valid @RequestBody ImagePresignRequest request) {
        ImagePresignResponse response = landmarkImageAdminService.presignUpload(landmarkId, request);
        return Result.success(response);
    }

    @PostMapping("/images/confirm")
    @Operation(summary = "确认图片上传", description = "前端上传完成后回传 key，后端校验文件并写入 image 表，返回 imageId")
    public Result<UUID> confirmUpload(
            @PathVariable UUID landmarkId,
            @Valid @RequestBody ImageConfirmRequest request) {
        UUID imageId = landmarkImageAdminService.confirmUpload(landmarkId, request);
        return Result.success(imageId);
    }

    @PutMapping("/cover")
    @Operation(summary = "设置封面图片", description = "从已有图片中选择一张设为地标封面")
    public Result<Void> setCover(
            @PathVariable UUID landmarkId,
            @Valid @RequestBody SetCoverRequest request) {
        landmarkImageAdminService.setCover(landmarkId, request.getImageId());
        return Result.success(null);
    }

    @PutMapping("/images/curated")
    @Operation(summary = "设置精选展示图片", description = "从已有图片中选择 1 到 5 张作为详情页精选展示")
    public Result<Void> setCuratedImages(
            @PathVariable UUID landmarkId,
            @Valid @RequestBody CuratedImagesRequest request) {
        landmarkImageAdminService.setCuratedImages(landmarkId, request.getImageIds());
        return Result.success(null);
    }

    @DeleteMapping("/images/{imageId}")
    @Operation(summary = "删除图片", description = "删除单张图片（MinIO 文件和数据库记录），自动清理封面/精选引用")
    public Result<Void> deleteImage(
            @PathVariable UUID landmarkId,
            @PathVariable UUID imageId) {
        try {
            landmarkImageAdminService.deleteImage(landmarkId, imageId);
            return Result.success(null);
        } catch (RuntimeException e) {
            String msg = e.getMessage();
            if (msg.contains("不存在") || msg.contains("不属于")) {
                return Result.failure(404, msg);
            }
            return Result.failure(400, msg);
        }
    }

    @DeleteMapping("/images/batch")
    @Operation(summary = "批量删除图片", description = "批量删除图片，返回影响的封面/精选数量")
    public Result<BatchDeleteImagesResponse> deleteImagesBatch(
            @PathVariable UUID landmarkId,
            @Valid @RequestBody BatchDeleteImagesRequest request) {
        try {
            BatchDeleteImagesResponse response = landmarkImageAdminService.batchDeleteImages(
                    landmarkId, request.getImageIds());
            return Result.success(response);
        } catch (RuntimeException e) {
            return Result.failure(400, e.getMessage());
        }
    }
}
