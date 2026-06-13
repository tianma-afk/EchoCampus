package com.echocampus.user.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.echocampus.user.vo.CheckinVO;

import java.util.UUID;

public interface CheckinService {

    void doCheckin(UUID userId, UUID landmarkId);

    Page<CheckinVO> getCheckinHistory(UUID userId, int page, int size);
}
