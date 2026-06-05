package com.echocampus.upload.service;

import com.echocampus.upload.vo.UploadPresignedUrlVO;

public interface UploadService {

    UploadPresignedUrlVO getPresignedUploadUrl();
}
