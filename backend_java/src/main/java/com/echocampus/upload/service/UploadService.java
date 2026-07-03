package com.echocampus.upload.service;

import com.echocampus.upload.vo.UploadPresignedUrlVO;
import org.springframework.web.multipart.MultipartFile;

public interface UploadService {

    UploadPresignedUrlVO getPresignedUploadUrl();

    UploadPresignedUrlVO uploadDirect(MultipartFile file);
}
