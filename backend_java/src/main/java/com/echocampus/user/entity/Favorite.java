package com.echocampus.user.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@TableName("favorite")
public class Favorite {

    @TableId
    private UUID id;

    private UUID userId;

    private UUID landmarkId;

    private OffsetDateTime createdAt;
}
