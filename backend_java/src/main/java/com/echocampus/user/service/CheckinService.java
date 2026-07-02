package com.echocampus.user.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.echocampus.user.vo.CheckinVO;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface CheckinService {

    void doCheckin(UUID userId, UUID landmarkId, BigDecimal latitude, BigDecimal longitude);

    Page<CheckinVO> getCheckinHistory(UUID userId, int page, int size);

    void batchDelete(UUID userId, List<UUID> ids);
}
