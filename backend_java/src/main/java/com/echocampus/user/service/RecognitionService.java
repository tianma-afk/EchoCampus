package com.echocampus.user.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.echocampus.user.vo.RecognitionVO;

import java.util.List;
import java.util.UUID;

public interface RecognitionService {

    Page<RecognitionVO> getRecognitionHistory(UUID userId, int page, int size);

    void batchDelete(UUID userId, List<UUID> ids);
}
