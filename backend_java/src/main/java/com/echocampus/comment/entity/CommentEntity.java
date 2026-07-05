package com.echocampus.comment.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@TableName("comment")
public class CommentEntity {

    @TableId
    private UUID id;

    private UUID landmarkId;

    private UUID userId;

    private UUID parentId;

    private String content;

    private Integer likeCount;

    private OffsetDateTime createdAt;
}
