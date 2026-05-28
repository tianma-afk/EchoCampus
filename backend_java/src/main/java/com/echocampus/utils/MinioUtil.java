package com.echocampus.utils;

import io.minio.*;
import io.minio.errors.*;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * MinIO 工具类 - 封装原生 SDK 操作
 * 职责：纯粹的 MinIO 技术操作，不包含业务逻辑
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MinioUtil {

    private final MinioClient minioClient;

    /**
     * 创建桶
     */
    public void createBucket(String bucketName) throws Exception {
        boolean exists = minioClient.bucketExists(
                BucketExistsArgs.builder().bucket(bucketName).build()
        );
        if (!exists) {
            minioClient.makeBucket(
                    MakeBucketArgs.builder().bucket(bucketName).build()
            );
            log.info("创建桶成功：{}", bucketName);
        }
    }

    /**
     * 检查桶是否存在
     */
    public boolean bucketExists(String bucketName) throws Exception {
        return minioClient.bucketExists(
                BucketExistsArgs.builder().bucket(bucketName).build()
        );
    }

    /**
     * 上传文件（InputStream 方式）
     */
    public ObjectWriteResponse uploadFile(
            String bucketName,
            String objectName,
            InputStream inputStream,
            String contentType,
            long size) throws Exception {

        PutObjectArgs args = PutObjectArgs.builder()
                .bucket(bucketName)
                .object(objectName)
                .stream(inputStream, size, -1)
                .contentType(contentType)
                .build();

        return minioClient.putObject(args);
    }

    /**
     * 上传文件（MultipartFile 方式）
     */
    public ObjectWriteResponse uploadFile(
            String bucketName,
            String objectName,
            MultipartFile file) throws Exception {

        PutObjectArgs args = PutObjectArgs.builder()
                .bucket(bucketName)
                .object(objectName)
                .stream(file.getInputStream(), file.getSize(), -1)
                .contentType(file.getContentType())
                .build();

        return minioClient.putObject(args);
    }

    /**
     * 下载文件
     */
    public InputStream downloadFile(String bucketName, String objectName) throws Exception {
        GetObjectArgs args = GetObjectArgs.builder()
                .bucket(bucketName)
                .object(objectName)
                .build();

        return minioClient.getObject(args);
    }

    /**
     * 检查对象是否存在
     */
//    public boolean isObjectExists(String bucketName, String objectName) throws Exception {
//        try {
//            minioClient.statObject(
//                    StatObjectArgs.builder()
//                            .bucket(bucketName)
//                            .object(objectName)
//                            .build()
//            );
//            return true;
//        } catch (ErrorResponseException e) {
//            if (e.code().equals("NoSuchKey")) {
//                return false;
//            }
//            throw e;
//        }
//    }

    /**
     * 删除对象
     */
    public void removeObject(String bucketName, String objectName) throws Exception {
        minioClient.removeObject(
                RemoveObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectName)
                        .build()
        );
    }

    /**
     * 设置桶策略
     */
    public void setBucketPolicy(String bucketName, String policyJson) throws Exception {
        minioClient.setBucketPolicy(
                SetBucketPolicyArgs.builder()
                        .bucket(bucketName)
                        .config(policyJson)
                        .build()
        );
    }

    /**
     * 获取预签名URL
     */
    public String getPresignedObjectUrl(
            String bucketName,
            String objectName,
            int expiry,
            TimeUnit timeUnit,
            Method method,
            String contentType) throws Exception {

        GetPresignedObjectUrlArgs.Builder builder = GetPresignedObjectUrlArgs.builder()
                .bucket(bucketName)
                .object(objectName)
                .expiry(expiry, timeUnit)
                .method(method);

        if (contentType != null) {
            builder.extraQueryParams(Map.of("content-type", contentType));
        }

        return minioClient.getPresignedObjectUrl(builder.build());
    }
}