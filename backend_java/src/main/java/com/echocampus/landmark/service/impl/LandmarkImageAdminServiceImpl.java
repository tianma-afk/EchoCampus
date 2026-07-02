package com.echocampus.landmark.service.impl;

import com.echocampus.shared.config.MinioConfig;
import com.echocampus.cleanup.service.CleanupService;
import com.echocampus.landmark.dto.ImageConfirmRequest;
import com.echocampus.landmark.dto.ImagePresignRequest;
import com.echocampus.landmark.dto.ImagePresignResponse;
import com.echocampus.campus.entity.CampusEntity;
import com.echocampus.landmark.entity.ImageEntity;
import com.echocampus.landmark.entity.LandmarkEntity;
import com.echocampus.university.entity.UniversityEntity;
import com.echocampus.campus.mapper.CampusMapper;
import com.echocampus.landmark.mapper.ImageMapper;
import com.echocampus.landmark.mapper.LandmarkMapper;
import com.echocampus.university.mapper.UniversityMapper;
import com.echocampus.landmark.service.LandmarkImageAdminService;
import com.echocampus.shared.exception.BusinessException;
import com.echocampus.shared.exception.ErrorCode;
import com.echocampus.shared.exception.TechnicalException;
import com.echocampus.shared.util.MinioUtil;
import com.echocampus.landmark.vo.BatchDeleteImagesResponse;
import com.echocampus.landmark.vo.ImagePageVO;
import com.echocampus.landmark.vo.LandmarkImageVO;
import io.minio.StatObjectResponse;
import io.minio.http.Method;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
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
    private final CleanupService cleanupService;
    private final String bucket;

    public LandmarkImageAdminServiceImpl(LandmarkMapper landmarkMapper, CampusMapper campusMapper,
                                          UniversityMapper universityMapper, ImageMapper imageMapper,
                                          MinioUtil minioUtil, MinioConfig minioConfig,
                                          CleanupService cleanupService) {
        this.landmarkMapper = landmarkMapper;
        this.campusMapper = campusMapper;
        this.universityMapper = universityMapper;
        this.imageMapper = imageMapper;
        this.minioUtil = minioUtil;
        this.bucket = minioConfig.getBucket();
        this.cleanupService = cleanupService;
    }

    @Override
    public ImagePresignResponse presignUpload(UUID landmarkId, ImagePresignRequest request) {
        LandmarkEntity landmark = landmarkMapper.selectById(landmarkId);
        if (landmark == null) {
            throw new BusinessException(ErrorCode.LANDMARK_NOT_FOUND);
        }

        CampusEntity campus = campusMapper.selectById(landmark.getCampusId());
        if (campus == null) {
            throw new BusinessException(ErrorCode.CAMPUS_NOT_FOUND);
        }

        UniversityEntity university = universityMapper.selectById(campus.getUniversityId());
        if (university == null) {
            throw new BusinessException(ErrorCode.UNIVERSITY_NOT_FOUND);
        }

        String ext = extractExtension(request.getFilename());
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "不支持的文件类型：" + ext);
        }

        UUID imageId = UUID.randomUUID();
        String key = String.format("imgs/%s/%s/%s/%s.%s",
                university.getId(), campus.getId(), landmarkId, imageId, ext);

        String presignedUrl;
        try {
            presignedUrl = minioUtil.getPresignedObjectUrl(
                    bucket, key, PRESIGN_UPLOAD_EXPIRY, TimeUnit.MINUTES, Method.PUT, mimeType(ext));
        } catch (Exception e) {
            throw new TechnicalException(ErrorCode.FILE_STORAGE_ERROR, e);
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
            throw new BusinessException(ErrorCode.LANDMARK_NOT_FOUND);
        }

        StatObjectResponse stat;
        try {
            stat = minioUtil.statObject(bucket, request.getKey());
        } catch (Exception e) {
            throw new TechnicalException(ErrorCode.FILE_STORAGE_ERROR, e);
        }

        if (stat.size() > MAX_FILE_SIZE) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "文件大小超过限制 10MB");
        }

        String contentType = stat.contentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "文件类型不是图片");
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
            throw new BusinessException(ErrorCode.LANDMARK_NOT_FOUND);
        }

        ImageEntity image = imageMapper.selectById(imageId);
        if (image == null || !image.getLandmarkId().equals(landmarkId)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "图片不属于该地标");
        }

        landmark.setCoverImageId(imageId);
        landmarkMapper.updateById(landmark);
    }

    @Override
    public void setCuratedImages(UUID landmarkId, List<UUID> imageIds) {
        if (imageIds != null && imageIds.size() > 5) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "精选图片最多 5 张");
        }

        LandmarkEntity landmark = landmarkMapper.selectById(landmarkId);
        if (landmark == null) {
            throw new BusinessException(ErrorCode.LANDMARK_NOT_FOUND);
        }

        List<ImageEntity> images = imageMapper.selectBatchIds(imageIds);
        if (images.size() != imageIds.size()) {
            throw new BusinessException(ErrorCode.IMAGE_NOT_FOUND);
        }
        for (ImageEntity image : images) {
            if (!image.getLandmarkId().equals(landmarkId)) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "图片不属于该地标");
            }
        }

        landmark.setImgs(imageIds);
        landmarkMapper.updateById(landmark);
    }

    @Override
    public ImagePageVO listImages(UUID landmarkId, int page, int pageSize) {
        LandmarkEntity landmark = landmarkMapper.selectById(landmarkId);
        if (landmark == null) {
            throw new BusinessException(ErrorCode.LANDMARK_NOT_FOUND);
        }

        CampusEntity campus = campusMapper.selectById(landmark.getCampusId());
        UniversityEntity university = null;
        if (campus != null) {
            university = universityMapper.selectById(campus.getUniversityId());
        }

        List<ImageEntity> allImages = imageMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ImageEntity>()
                        .eq(ImageEntity::getLandmarkId, landmarkId)
                        .orderByDesc(ImageEntity::getCreatedAt));

        UUID coverImageId = landmark.getCoverImageId();
        List<UUID> curatedIds = landmark.getImgs() != null ? landmark.getImgs() : List.of();

        // Build VOs and split into pinned / normal sets
        java.util.Map<UUID, LandmarkImageVO> voMap = new java.util.HashMap<>();
        for (ImageEntity image : allImages) {
            String url = null;
            if (university != null && campus != null) {
                String key = String.format("imgs/%s/%s/%s/%s.%s",
                        university.getId(), campus.getId(), landmarkId, image.getId(), image.getFileExt());
                try {
                    url = minioUtil.getPresignedObjectUrl(bucket, key, 30, TimeUnit.MINUTES, Method.GET, null);
                } catch (Exception ignored) {
                }
            }
            voMap.put(image.getId(), LandmarkImageVO.builder()
                    .id(image.getId())
                    .url(url)
                    .isCover(coverImageId != null && coverImageId.equals(image.getId()))
                    .isCurated(curatedIds.contains(image.getId()))
                    .fileExt(image.getFileExt())
                    .createdAt(image.getCreatedAt())
                    .build());
        }

        // Build ordered list: cover first → curated by imgs order → rest by createdAt desc
        LinkedHashSet<UUID> orderedIds = new LinkedHashSet<>();

        // cover first
        if (coverImageId != null && voMap.containsKey(coverImageId)) {
            orderedIds.add(coverImageId);
        }

        // curated by imgs order
        for (UUID curatedId : curatedIds) {
            if (voMap.containsKey(curatedId)) {
                orderedIds.add(curatedId);
            }
        }

        // rest by createdAt desc (allImages is already in that order)
        for (ImageEntity image : allImages) {
            orderedIds.add(image.getId());
        }

        List<LandmarkImageVO> orderedList = new ArrayList<>();
        for (UUID id : orderedIds) {
            LandmarkImageVO vo = voMap.get(id);
            if (vo != null) {
                orderedList.add(vo);
            }
        }

        long total = orderedList.size();
        int fromIndex = (page - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, (int) total);
        List<LandmarkImageVO> pageRecords;
        if (fromIndex >= total) {
            pageRecords = List.of();
        } else {
            pageRecords = orderedList.subList(fromIndex, toIndex);
        }

        return ImagePageVO.builder()
                .records(pageRecords)
                .total(total)
                .hasMore(toIndex < total)
                .build();
    }

    @Override
    public void deleteImage(UUID landmarkId, UUID imageId) {
        LandmarkEntity landmark = landmarkMapper.selectById(landmarkId);
        if (landmark == null) {
            throw new BusinessException(ErrorCode.LANDMARK_NOT_FOUND);
        }

        ImageEntity image = imageMapper.selectById(imageId);
        if (image == null || !image.getLandmarkId().equals(landmarkId)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "图片不属于该地标");
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

        // 已向量化的图片，加入 Milvus 待删队列
        if (Boolean.TRUE.equals(image.getIsVectorized())) {
            cleanupService.addPending(imageId);
        }
    }

    @Override
    public BatchDeleteImagesResponse batchDeleteImages(UUID landmarkId, List<UUID> imageIds) {
        LandmarkEntity landmark = landmarkMapper.selectById(landmarkId);
        if (landmark == null) {
            throw new BusinessException(ErrorCode.LANDMARK_NOT_FOUND);
        }

        boolean affectedCover = false;
        int affectedCuratedCount = 0;

        if (landmark.getCoverImageId() != null && imageIds.contains(landmark.getCoverImageId())) {
            affectedCover = true;
        }
        if (landmark.getImgs() != null) {
            for (UUID imgId : imageIds) {
                if (landmark.getImgs().contains(imgId)) {
                    affectedCuratedCount++;
                }
            }
        }

        CampusEntity campus = campusMapper.selectById(landmark.getCampusId());
        UniversityEntity university = null;
        if (campus != null) {
            university = universityMapper.selectById(campus.getUniversityId());
        }

        for (UUID imageId : imageIds) {
            ImageEntity image = imageMapper.selectById(imageId);
            if (image == null || !image.getLandmarkId().equals(landmarkId)) {
                continue;
            }

            // delete from MinIO
            if (university != null && campus != null) {
                String key = String.format("imgs/%s/%s/%s/%s.%s",
                        university.getId(), campus.getId(), landmarkId, image.getId(), image.getFileExt());
                try {
                    minioUtil.removeObject(bucket, key);
                } catch (Exception ignored) {
                }
            }

            imageMapper.deleteById(imageId);

            // 已向量化的图片，加入 Milvus 待删队列
            if (Boolean.TRUE.equals(image.getIsVectorized())) {
                cleanupService.addPending(imageId);
            }
        }

        // clean up landmark references
        if (affectedCover) {
            landmark.setCoverImageId(null);
        }
        if (affectedCuratedCount > 0 && landmark.getImgs() != null) {
            List<UUID> updatedImgs = new ArrayList<>(landmark.getImgs());
            updatedImgs.removeAll(imageIds);
            landmark.setImgs(updatedImgs);
        }
        landmarkMapper.updateById(landmark);

        return BatchDeleteImagesResponse.builder()
                .deletedCount(imageIds.size())
                .affectedCover(affectedCover)
                .affectedCuratedCount(affectedCuratedCount)
                .build();
    }

    private String extractExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex == -1 || dotIndex == filename.length() - 1) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "无法识别文件扩展名");
        }
        return filename.substring(dotIndex + 1).toLowerCase();
    }
}
