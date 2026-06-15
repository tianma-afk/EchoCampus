package com.echocampus.user.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@TableName("rating")
public class Rating {

    @TableId
    private UUID id;

    private UUID userId;

    private UUID landmarkId;

    private BigDecimal rating;

    private OffsetDateTime createdAt;

    private OffsetDateTime updatedAt;
}
