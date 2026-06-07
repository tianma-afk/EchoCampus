package com.echocampus.user.entity;

import java.time.OffsetDateTime;
import java.util.UUID;

import lombok.Data;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@Data
@TableName("\"user\"")
public class UserEntity {

    @TableId
    private UUID id;

    private String nickname;

    private String email;

    private String passwordHash;

    private OffsetDateTime createdAt;
}