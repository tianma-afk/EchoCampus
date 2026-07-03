package com.echocampus.upload.service.impl;

import com.echocampus.shared.config.MinioConfig;
import com.echocampus.upload.service.UploadService;
import com.echocampus.shared.exception.ErrorCode;
import com.echocampus.shared.exception.TechnicalException;
import com.echocampus.shared.util.MinioUtil;
import com.echocampus.upload.vo.UploadPresignedUrlVO;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class UploadServiceImpl implements UploadService {

    private static final String BUCKET_NAME = "campus";
    private static final String IMAGE_PREFIX = "images";
    private static final String DEFAULT_EXTENSION = ".jpg";
    private static final int PRESIGNED_EXPIRY = 10;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    private static final String PUBLIC_READ_POLICY = """
            {
              "Version": "2012-10-17",
              "Statement": [{
                "Effect": "Allow",
                "Principal": { "AWS": ["*"] },
                "Action": ["s3:GetObject"],
                "Resource": ["arn:aws:s3:::campus/*"]
              }]
            }
            """;

    private final MinioUtil minioUtil;
    private final MinioConfig minioConfig;

    @Value("${api.public-base:http://localhost:8080}")
    private String apiPublicBase;

    @Override
    public UploadPresignedUrlVO getPresignedUploadUrl() {
        try {
            ensureBucketExists();

            String objectName = generateObjectName();
            String uploadUrl = minioUtil.getPresignedObjectUrl(
                    BUCKET_NAME, objectName,
                    PRESIGNED_EXPIRY, TimeUnit.MINUTES,
                    Method.PUT, null
            );
            uploadUrl = uploadUrl.replace(minioConfig.getEndpoint(), minioConfig.getPublicEndpoint());

            log.info("生成预签名上传URL: bucket={}, objectName={}", BUCKET_NAME, objectName);

            return UploadPresignedUrlVO.builder()
                    .uploadUrl(uploadUrl)
                    .bucket(BUCKET_NAME)
                    .objectName(objectName)
                    .build();
        } catch (Exception e) {
            log.error("生成预签名上传URL失败", e);
            throw new TechnicalException(ErrorCode.FILE_STORAGE_ERROR, e);
        }
    }

    @Override
    public UploadPresignedUrlVO uploadDirect(MultipartFile file) {
        try {
            ensureBucketExists();

            String objectName = generateObjectName();
            minioUtil.uploadFile(BUCKET_NAME, objectName, file);

            String imageUrl = String.format("%s/api/v1/upload/files?bucket=%s&object=%s",
                    apiPublicBase, BUCKET_NAME, objectName);

            return UploadPresignedUrlVO.builder()
                    .uploadUrl(imageUrl)
                    .bucket(BUCKET_NAME)
                    .objectName(objectName)
                    .build();
        } catch (Exception e) {
            log.error("直传失败", e);
            throw new TechnicalException(ErrorCode.FILE_STORAGE_ERROR, e);
        }
    }

    private void ensureBucketExists() throws Exception {
        boolean exists = minioUtil.bucketExists(BUCKET_NAME);
        if (!exists) {
            minioUtil.createBucket(BUCKET_NAME);
            log.info("自动创建MinIO桶: {}", BUCKET_NAME);
        }
        minioUtil.setBucketPolicy(BUCKET_NAME, PUBLIC_READ_POLICY);
    }

    private String generateObjectName() {
        String datePath = LocalDate.now().format(DATE_FORMATTER);
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        return IMAGE_PREFIX + "/" + datePath + "/" + uuid + DEFAULT_EXTENSION;
    }
}
