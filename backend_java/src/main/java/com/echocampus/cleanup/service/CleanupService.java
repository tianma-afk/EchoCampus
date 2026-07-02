package com.echocampus.cleanup.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.echocampus.cleanup.entity.MilvusPendingDeleteEntity;

import java.util.List;
import java.util.UUID;

public interface CleanupService {

    void addPending(UUID imageId);

    Page<MilvusPendingDeleteEntity> listPending(int page, int size);

    void batchDelete(List<UUID> ids);
}
