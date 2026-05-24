package com.echocampus.service.admin.impl;

import com.echocampus.config.MinioConfig;
import com.echocampus.dto.ImageConfirmRequest;
import com.echocampus.dto.ImagePresignRequest;
import com.echocampus.dto.ImagePresignResponse;
import com.echocampus.entity.CampusEntity;
import com.echocampus.entity.ImageEntity;
import com.echocampus.entity.LandmarkEntity;
import com.echocampus.entity.UniversityEntity;
import com.echocampus.mapper.CampusMapper;
import com.echocampus.mapper.ImageMapper;
import com.echocampus.mapper.LandmarkMapper;
import com.echocampus.mapper.UniversityMapper;
import com.echocampus.service.admin.LandmarkImageAdminService;
import com.echocampus.utils.MinioUtil;
import io.minio.StatObjectResponse;
import io.minio.http.Method;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class LandmarkImageAdminServiceImpl implements LandmarkImageAdminService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;
    private static final int PRESIGN_UPLOAD_EXPIRY = 10;

    private final LandmarkMapper landmarkMapper;
    private final CampusMapper campusMapper;
    private final UniversityMapper universityMapper;
    private final ImageMapper imageMapper;
    private final MinioUtil minioUtil;
    private final String bucket;

    public LandmarkImageAdminServiceImpl(LandmarkMapper landmarkMapper, CampusMapper campusMapper,
                                         UniversityMapper universityMapper, ImageMapper imageMapper,
                                         MinioUtil minioUtil, MinioConfig minioConfig) {
        this.landmarkMapper = landmarkMapper;
        this.campusMapper = campusMapper;
        this.universityMapper = universityMapper;
        this.imageMapper = imageMapper;
        this.minioUtil = minioUtil;
        this.bucket = minioConfig.getBucket();
    }

    @Override
    public ImagePresignResponse presignUpload(UUID landmarkId, ImagePresignRequest request) {
        LandmarkEntity landmark = landmarkMapper.selectById(landmarkId);
        if (landmark == null) {
            throw new RuntimeException("地标不存在");
        }

        CampusEntity campus = campusMapper.selectById(landmark.getCampusId());
        if (campus == null) {
            throw new RuntimeException("校区不存在");
        }

        UniversityEntity university = universityMapper.selectById(campus.getUniversityId());
        if (university == null) {
            throw new RuntimeException("大学不存在");
        }

        String ext = extractExtension(request.getFilename());
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new RuntimeException("不支持的文件类型：" + ext);
        }

        UUID imageId = UUID.randomUUID();
        String key = String.format("imgs/%s/%s/%s/%s.%s",
                university.getId(), campus.getId(), landmarkId, imageId, ext);

        String presignedUrl;
        try {
            presignedUrl = minioUtil.getPresignedObjectUrl(
                    bucket, key, PRESIGN_UPLOAD_EXPIRY, TimeUnit.MINUTES, Method.PUT, "image/" + ext);
        } catch (Exception e) {
            throw new RuntimeException("生成预签名 URL 失败", e);
        }

        ImagePresignResponse response = new ImagePresignResponse();
        response.setKey(key);
        response.setPresignedUrl(presignedUrl);
        return response;
    }

    @Override
    public UUID confirmUpload(UUID landmarkId, ImageConfirmRequest request) {
        LandmarkEntity landmark = landmarkMapper.selectById(landmarkId);
        if (landmark == null) {
            throw new RuntimeException("地标不存在");
        }

        StatObjectResponse stat;
        try {
            stat = minioUtil.statObject(bucket, request.getKey());
        } catch (Exception e) {
            throw new RuntimeException("文件不存在或无法访问", e);
        }

        if (stat.size() > MAX_FILE_SIZE) {
            throw new RuntimeException("文件大小超过限制 10MB");
        }

        String contentType = stat.contentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new RuntimeException("文件类型不是图片");
        }

        String ext = extractExtension(request.getKey());

        ImageEntity image = new ImageEntity();
        image.setLandmarkId(landmarkId);
        image.setIsVectorized(false);
        image.setFileExt(ext);
        imageMapper.insert(image);

        return image.getId();
    }

    @Override
    public void setCover(UUID landmarkId, UUID imageId) {
        LandmarkEntity landmark = landmarkMapper.selectById(landmarkId);
        if (landmark == null) {
            throw new RuntimeException("地标不存在");
        }

        ImageEntity image = imageMapper.selectById(imageId);
        if (image == null || !image.getLandmarkId().equals(landmarkId)) {
            throw new RuntimeException("图片不属于该地标");
        }

        landmark.setCoverImageId(imageId);
        landmarkMapper.updateById(landmark);
    }

    @Override
    public void setCuratedImages(UUID landmarkId, List<UUID> imageIds) {
        LandmarkEntity landmark = landmarkMapper.selectById(landmarkId);
        if (landmark == null) {
            throw new RuntimeException("地标不存在");
        }

        List<ImageEntity> images = imageMapper.selectBatchIds(imageIds);
        if (images.size() != imageIds.size()) {
            throw new RuntimeException("部分图片不存在");
        }
        for (ImageEntity image : images) {
            if (!image.getLandmarkId().equals(landmarkId)) {
                throw new RuntimeException("图片不属于该地标");
            }
        }

        landmark.setImgs(imageIds);
        landmarkMapper.updateById(landmark);
    }

    private String extractExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex == -1 || dotIndex == filename.length() - 1) {
            throw new RuntimeException("无法识别文件扩展名");
        }
        return filename.substring(dotIndex + 1).toLowerCase();
    }
}
