package com.echocampus.cleanup.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.echocampus.cleanup.entity.MilvusPendingDeleteEntity;
import com.echocampus.cleanup.mapper.MilvusPendingDeleteMapper;
import com.echocampus.cleanup.service.CleanupService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class CleanupServiceImpl implements CleanupService {

    private final MilvusPendingDeleteMapper pendingMapper;

    public CleanupServiceImpl(MilvusPendingDeleteMapper pendingMapper) {
        this.pendingMapper = pendingMapper;
    }

    @Override
    public void addPending(UUID imageId) {
        MilvusPendingDeleteEntity entity = new MilvusPendingDeleteEntity(
                UUID.randomUUID(), imageId, LocalDateTime.now());
        pendingMapper.insert(entity);
        log.info("[图片清理] 加入待删队列: imageId={}", imageId);
    }

    @Override
    public Page<MilvusPendingDeleteEntity> listPending(int page, int size) {
        Page<MilvusPendingDeleteEntity> entityPage = new Page<>(page, size);
        pendingMapper.selectPage(entityPage, new LambdaQueryWrapper<MilvusPendingDeleteEntity>()
                .orderByDesc(MilvusPendingDeleteEntity::getCreatedAt));
        return entityPage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDelete(List<UUID> ids) {
        if (ids.isEmpty()) return;
        pendingMapper.delete(new LambdaQueryWrapper<MilvusPendingDeleteEntity>()
                .in(MilvusPendingDeleteEntity::getId, ids));
        log.info("[图片清理] 从待删队列移除: count={}", ids.size());
    }
}
