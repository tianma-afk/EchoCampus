package com.echocampus.comment.service;

import com.echocampus.comment.vo.CommentVO;

import java.util.List;
import java.util.UUID;

public interface CommentService {

    void submitComment(String userId, UUID landmarkId, UUID parentId, String content);

    List<CommentVO> getCommentsByLandmark(UUID landmarkId, String currentUserId);

    void likeComment(UUID commentId, String userId);

    void unlikeComment(UUID commentId, String userId);
}
