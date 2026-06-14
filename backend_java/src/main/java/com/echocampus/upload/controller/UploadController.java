package com.echocampus.upload.controller;

import com.echocampus.shared.annotation.RequireRole;
import com.echocampus.shared.enums.RoleEnum;
import com.echocampus.upload.service.UploadService;
import com.echocampus.shared.vo.Result;
import com.echocampus.upload.vo.UploadPresignedUrlVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/upload")
@RequireRole(RoleEnum.USER)
@Tag(name = "文件上传", description = "提供图片上传预签名URL")
public class UploadController {

    private final UploadService uploadService;

    public UploadController(UploadService uploadService) {
        this.uploadService = uploadService;
    }

    @PostMapping("/presigned-url")
    @Operation(summary = "获取预签名上传URL", description = "返回MinIO预签名上传URL，前端使用该URL直传图片")
    public Result<UploadPresignedUrlVO> getPresignedUploadUrl() {
        return Result.success(uploadService.getPresignedUploadUrl());
    }
}
