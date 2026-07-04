package com.echocampus.comment.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentVO {
    private UUID id;
    private UUID landmarkId;
    private UUID userId;
    private String nickname;
    private UUID parentId;
    private String content;
    private Integer likeCount;
    private Boolean isLiked;
    private OffsetDateTime createdAt;
    private List<CommentVO> replies;
}
