package com.echocampus.controller.admin;

import com.echocampus.dto.CuratedImagesRequest;
import com.echocampus.dto.ImageConfirmRequest;
import com.echocampus.dto.ImagePresignRequest;
import com.echocampus.dto.ImagePresignResponse;
import com.echocampus.dto.SetCoverRequest;
import com.echocampus.service.admin.LandmarkImageAdminService;
import com.echocampus.vo.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/landmarks/{landmarkId}")
@Tag(name = "管理员地标图片", description = "提供地标图片的上传和管理功能")
public class LandmarkImageAdminController {
    private final LandmarkImageAdminService landmarkImageAdminService;

    public LandmarkImageAdminController(LandmarkImageAdminService landmarkImageAdminService) {
        this.landmarkImageAdminService = landmarkImageAdminService;
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
    @Operation(summary = "设置精选展示图片", description = "从已有图片中选择 3-4 张作为详情页精选展示")
    public Result<Void> setCuratedImages(
            @PathVariable UUID landmarkId,
            @Valid @RequestBody CuratedImagesRequest request) {
        landmarkImageAdminService.setCuratedImages(landmarkId, request.getImageIds());
        return Result.success(null);
    }
}
