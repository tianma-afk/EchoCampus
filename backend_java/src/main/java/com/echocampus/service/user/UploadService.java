package com.echocampus.service.user;

import com.echocampus.vo.UploadPresignedUrlVO;

public interface UploadService {

    UploadPresignedUrlVO getPresignedUploadUrl();
}
