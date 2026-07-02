package com.echocampus.cleanup.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;
import java.util.UUID;

@TableName("milvus_pending_delete")
public class MilvusPendingDeleteEntity {

    @TableId
    private UUID id;
    private UUID imageId;
    private LocalDateTime createdAt;

    public MilvusPendingDeleteEntity() {}

    public MilvusPendingDeleteEntity(UUID id, UUID imageId, LocalDateTime createdAt) {
        this.id = id;
        this.imageId = imageId;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getImageId() { return imageId; }
    public void setImageId(UUID imageId) { this.imageId = imageId; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
