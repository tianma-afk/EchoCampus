package com.echocampus.admin.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@TableName("admin")
public class AdminEntity {

    @TableId
    private UUID id;

    private String username;

    private String passwordHash;

    private String email;

    private OffsetDateTime createdAt;

    private OffsetDateTime updatedAt;
}
