package com.echocampus.user.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.echocampus.user.vo.RecognitionVO;

import java.util.UUID;

public interface RecognitionService {

    Page<RecognitionVO> getRecognitionHistory(UUID userId, int page, int size);
}
