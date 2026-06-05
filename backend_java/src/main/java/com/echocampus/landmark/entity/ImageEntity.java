package com.echocampus.landmark.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@TableName("image")
public class ImageEntity {

    @TableId
    private UUID id;

    private UUID landmarkId;

    private Boolean isVectorized;

    private String fileExt;

    private OffsetDateTime createdAt;
}
