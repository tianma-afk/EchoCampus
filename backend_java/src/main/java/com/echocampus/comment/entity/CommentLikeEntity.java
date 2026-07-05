package com.echocampus.comment.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@TableName("comment_like")
public class CommentLikeEntity {

    @TableId
    private UUID id;

    private UUID commentId;

    private UUID userId;

    private OffsetDateTime createdAt;
}
