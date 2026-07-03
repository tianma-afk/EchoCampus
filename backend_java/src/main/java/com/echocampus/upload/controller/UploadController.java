package com.echocampus.upload.controller;

import com.echocampus.shared.annotation.RequireRole;
import com.echocampus.shared.enums.RoleEnum;
import com.echocampus.upload.service.UploadService;
import com.echocampus.shared.vo.Result;
import com.echocampus.shared.util.MinioUtil;
import com.echocampus.upload.vo.UploadPresignedUrlVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/upload")
@Tag(name = "文件上传", description = "提供图片上传预签名URL")
public class UploadController {

    private final UploadService uploadService;
    private final MinioUtil minioUtil;

    public UploadController(UploadService uploadService, MinioUtil minioUtil) {
        this.uploadService = uploadService;
        this.minioUtil = minioUtil;
    }

    @PostMapping("/presigned-url")
    @RequireRole(RoleEnum.USER)
    @Operation(summary = "获取预签名上传URL", description = "返回MinIO预签名上传URL，前端使用该URL直传图片")
    public Result<UploadPresignedUrlVO> getPresignedUploadUrl() {
        return Result.success(uploadService.getPresignedUploadUrl());
    }

    @PostMapping("/direct")
    @RequireRole(RoleEnum.USER)
    @Operation(summary = "直接上传图片", description = "前端通过multipart POST直接上传图片到后端，后端转发到MinIO")
    public Result<UploadPresignedUrlVO> uploadDirect(@RequestParam("file") MultipartFile file) {
        return Result.success(uploadService.uploadDirect(file));
    }

    @GetMapping("/files")
    @Operation(summary = "获取图片文件", description = "通过bucket和objectName获取存储在MinIO中的图片")
    public ResponseEntity<byte[]> getFile(
            @RequestParam String bucket,
            @RequestParam String object) {
        try {
            var stat = minioUtil.statObject(bucket, object);
            String contentType = stat.contentType();
            if (contentType == null || contentType.isBlank()) {
                contentType = "image/jpeg";
            }
            try (var in = minioUtil.downloadFile(bucket, object)) {
                byte[] bytes = in.readAllBytes();
                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .contentLength(bytes.length)
                        .body(bytes);
            }
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}
