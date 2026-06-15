package com.echocampus.user.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@TableName("user_image")
public class UserImage {

    @TableId
    private UUID id;

    private UUID userId;

    private String objectName;

    private String fileExt;

    private OffsetDateTime createdAt;
}
