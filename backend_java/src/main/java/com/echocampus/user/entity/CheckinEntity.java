package com.echocampus.user.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@TableName("checkin")
public class CheckinEntity {

    @TableId
    private UUID id;

    private UUID userId;

    private UUID landmarkId;

    private LocalDateTime createdAt;
}
