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
import com.echocampus.vo.LandmarkImageVO;
import io.minio.StatObjectResponse;
import io.minio.http.Method;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class LandmarkImageAdminServiceImpl implements LandmarkImageAdminService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;
    private static final int PRESIGN_UPLOAD_EXPIRY = 10;

    private static String mimeType(String ext) {
        return switch (ext) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "webp" -> "image/webp";
            default -> "application/octet-stream";
        };
    }

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
                    bucket, key, PRESIGN_UPLOAD_EXPIRY, TimeUnit.MINUTES, Method.PUT, mimeType(ext));
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

        // Parse the imageId from the presigned key so the DB record matches
        // the MinIO object path. Key format: imgs/univId/campusId/landmarkId/imageId.ext
        String filename = request.getKey().substring(request.getKey().lastIndexOf('/') + 1);
        UUID imageId = UUID.fromString(filename.substring(0, filename.lastIndexOf('.')));

        ImageEntity image = new ImageEntity();
        image.setId(imageId);
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

    @Override
    public List<LandmarkImageVO> listImages(UUID landmarkId) {
        LandmarkEntity landmark = landmarkMapper.selectById(landmarkId);
        if (landmark == null) {
            throw new RuntimeException("地标不存在");
        }

        CampusEntity campus = campusMapper.selectById(landmark.getCampusId());
        UniversityEntity university = null;
        if (campus != null) {
            university = universityMapper.selectById(campus.getUniversityId());
        }

        List<ImageEntity> images = imageMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ImageEntity>()
                        .eq(ImageEntity::getLandmarkId, landmarkId)
                        .orderByDesc(ImageEntity::getCreatedAt));

        UUID coverImageId = landmark.getCoverImageId();
        List<UUID> curatedIds = landmark.getImgs() != null ? landmark.getImgs() : List.of();

        List<LandmarkImageVO> result = new ArrayList<>();
        for (ImageEntity image : images) {
            String url = null;
            if (university != null && campus != null) {
                String key = String.format("imgs/%s/%s/%s/%s.%s",
                        university.getId(), campus.getId(), landmarkId, image.getId(), image.getFileExt());
                try {
                    url = minioUtil.getPresignedObjectUrl(bucket, key, 30, TimeUnit.MINUTES, Method.GET, null);
                } catch (Exception ignored) {
                }
            }
            result.add(LandmarkImageVO.builder()
                    .id(image.getId())
                    .url(url)
                    .isCover(coverImageId != null && coverImageId.equals(image.getId()))
                    .isCurated(curatedIds.contains(image.getId()))
                    .fileExt(image.getFileExt())
                    .createdAt(image.getCreatedAt())
                    .build());
        }
        return result;
    }

    @Override
    public void deleteImage(UUID landmarkId, UUID imageId) {
        LandmarkEntity landmark = landmarkMapper.selectById(landmarkId);
        if (landmark == null) {
            throw new RuntimeException("地标不存在");
        }

        ImageEntity image = imageMapper.selectById(imageId);
        if (image == null || !image.getLandmarkId().equals(landmarkId)) {
            throw new RuntimeException("图片不属于该地标");
        }

        // delete from MinIO
        CampusEntity campus = campusMapper.selectById(landmark.getCampusId());
        if (campus != null) {
            UniversityEntity university = universityMapper.selectById(campus.getUniversityId());
            if (university != null) {
                String key = String.format("imgs/%s/%s/%s/%s.%s",
                        university.getId(), campus.getId(), landmarkId, image.getId(), image.getFileExt());
                try {
                    minioUtil.removeObject(bucket, key);
                } catch (Exception ignored) {
                }
            }
        }

        // clean up landmark references
        if (landmark.getCoverImageId() != null && landmark.getCoverImageId().equals(imageId)) {
            landmark.setCoverImageId(null);
        }
        if (landmark.getImgs() != null && landmark.getImgs().contains(imageId)) {
            List<UUID> updatedImgs = new ArrayList<>(landmark.getImgs());
            updatedImgs.remove(imageId);
            landmark.setImgs(updatedImgs);
        }
        landmarkMapper.updateById(landmark);

        // delete from DB
        imageMapper.deleteById(imageId);
    }

    private String extractExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex == -1 || dotIndex == filename.length() - 1) {
            throw new RuntimeException("无法识别文件扩展名");
        }
        return filename.substring(dotIndex + 1).toLowerCase();
    }
}
