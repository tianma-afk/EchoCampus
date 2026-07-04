package com.echocampus.comment.controller;

import com.echocampus.comment.dto.SubmitCommentRequest;
import com.echocampus.comment.service.CommentService;
import com.echocampus.comment.vo.CommentVO;
import com.echocampus.shared.annotation.RequireRole;
import com.echocampus.shared.context.AuthContext;
import com.echocampus.shared.enums.RoleEnum;
import com.echocampus.shared.exception.ErrorCode;
import com.echocampus.shared.vo.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/user/comments")
@RequireRole(RoleEnum.USER)
@Tag(name = "用户评论", description = "社区评论功能")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @GetMapping
    @Operation(summary = "获取地标评论列表")
    public Result<List<CommentVO>> getComments(@RequestParam UUID landmarkId) {
        String userId = AuthContext.get().getUserId();
        return Result.success(commentService.getCommentsByLandmark(landmarkId, userId));
    }

    @PostMapping
    @Operation(summary = "提交评论或回复")
    public Result<Void> submitComment(@Valid @RequestBody SubmitCommentRequest request) {
        String userId = AuthContext.get().getUserId();
        commentService.submitComment(userId, request.getLandmarkId(), request.getParentId(), request.getContent());
        return Result.success(null);
    }

    @PostMapping("/{id}/like")
    @Operation(summary = "点赞评论")
    public Result<Void> likeComment(@PathVariable UUID id) {
        String userId = AuthContext.get().getUserId();
        commentService.likeComment(id, userId);
        return Result.success(null);
    }

    @DeleteMapping("/{id}/like")
    @Operation(summary = "取消点赞评论")
    public Result<Void> unlikeComment(@PathVariable UUID id) {
        String userId = AuthContext.get().getUserId();
        commentService.unlikeComment(id, userId);
        return Result.success(null);
    }
}
