package com.echocampus.comment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.echocampus.comment.entity.CommentEntity;
import com.echocampus.comment.entity.CommentLikeEntity;
import com.echocampus.comment.mapper.CommentLikeMapper;
import com.echocampus.comment.mapper.CommentMapper;
import com.echocampus.comment.service.CommentService;
import com.echocampus.comment.vo.CommentVO;
import com.echocampus.shared.exception.BusinessException;
import com.echocampus.shared.exception.ErrorCode;
import com.echocampus.user.entity.UserEntity;
import com.echocampus.user.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CommentServiceImpl implements CommentService {

    private final CommentMapper commentMapper;
    private final CommentLikeMapper commentLikeMapper;
    private final UserMapper userMapper;

    public CommentServiceImpl(CommentMapper commentMapper, CommentLikeMapper commentLikeMapper,
                               UserMapper userMapper) {
        this.commentMapper = commentMapper;
        this.commentLikeMapper = commentLikeMapper;
        this.userMapper = userMapper;
    }

    @Override
    public void submitComment(String userId, UUID landmarkId, UUID parentId, String content) {
        CommentEntity comment = new CommentEntity();
        comment.setId(UUID.randomUUID());
        comment.setLandmarkId(landmarkId);
        comment.setUserId(UUID.fromString(userId));
        comment.setParentId(parentId);
        comment.setContent(content.trim());
        comment.setLikeCount(0);
        comment.setCreatedAt(OffsetDateTime.now());
        commentMapper.insert(comment);
        log.info("评论提交成功: commentId={}, userId={}, landmarkId={}", comment.getId(), userId, landmarkId);
    }

    @Override
    public List<CommentVO> getCommentsByLandmark(UUID landmarkId, String currentUserId) {
        List<CommentEntity> allComments = commentMapper.selectList(
                new LambdaQueryWrapper<CommentEntity>()
                        .eq(CommentEntity::getLandmarkId, landmarkId)
                        .orderByDesc(CommentEntity::getCreatedAt));

        if (allComments.isEmpty()) {
            return Collections.emptyList();
        }

        // 分离顶级评论和回复
        List<CommentEntity> topLevel = allComments.stream()
                .filter(c -> c.getParentId() == null)
                .collect(Collectors.toList());
        List<CommentEntity> replies = allComments.stream()
                .filter(c -> c.getParentId() != null)
                .collect(Collectors.toList());

        // 收集所有用户ID
        Set<UUID> userIds = new HashSet<>();
        allComments.forEach(c -> userIds.add(c.getUserId()));

        // 批量查用户昵称
        Map<UUID, String> nicknameMap = userIds.isEmpty() ? new HashMap<>()
                : userMapper.selectBatchIds(userIds).stream()
                        .collect(Collectors.toMap(UserEntity::getId, UserEntity::getNickname, (a, b) -> a));

        // 查当前用户的点赞状态
        Set<UUID> likedCommentIds = Collections.emptySet();
        if (currentUserId != null) {
            List<UUID> allCommentIds = allComments.stream().map(CommentEntity::getId).toList();
            List<CommentLikeEntity> myLikes = commentLikeMapper.selectList(
                    new LambdaQueryWrapper<CommentLikeEntity>()
                            .in(CommentLikeEntity::getCommentId, allCommentIds)
                            .eq(CommentLikeEntity::getUserId, UUID.fromString(currentUserId)));
            likedCommentIds = myLikes.stream().map(CommentLikeEntity::getCommentId).collect(Collectors.toSet());
        }

        // 回复按 parentId 分组
        Map<UUID, List<CommentEntity>> replyMap = replies.stream()
                .collect(Collectors.groupingBy(CommentEntity::getParentId));

        Set<UUID> finalLikedCommentIds = likedCommentIds;
        return topLevel.stream().map(c -> {
            List<CommentVO> subReplies = replyMap.getOrDefault(c.getId(), Collections.emptyList())
                    .stream()
                    .map(r -> toVO(r, nicknameMap, finalLikedCommentIds))
                    .collect(Collectors.toList());

            CommentVO vo = toVO(c, nicknameMap, finalLikedCommentIds);
            vo.setReplies(subReplies);
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void likeComment(UUID commentId, String userId) {
        CommentEntity comment = commentMapper.selectById(commentId);
        if (comment == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "评论不存在");
        }

        CommentLikeEntity existing = commentLikeMapper.selectOne(
                new LambdaQueryWrapper<CommentLikeEntity>()
                        .eq(CommentLikeEntity::getCommentId, commentId)
                        .eq(CommentLikeEntity::getUserId, UUID.fromString(userId)));
        if (existing != null) {
            return; // 已点赞，幂等
        }

        CommentLikeEntity like = new CommentLikeEntity();
        like.setId(UUID.randomUUID());
        like.setCommentId(commentId);
        like.setUserId(UUID.fromString(userId));
        like.setCreatedAt(OffsetDateTime.now());
        commentLikeMapper.insert(like);

        comment.setLikeCount(comment.getLikeCount() + 1);
        commentMapper.updateById(comment);
        log.info("点赞成功: commentId={}, userId={}", commentId, userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unlikeComment(UUID commentId, String userId) {
        CommentEntity comment = commentMapper.selectById(commentId);
        if (comment == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "评论不存在");
        }

        int deleted = commentLikeMapper.delete(
                new LambdaQueryWrapper<CommentLikeEntity>()
                        .eq(CommentLikeEntity::getCommentId, commentId)
                        .eq(CommentLikeEntity::getUserId, UUID.fromString(userId)));
        if (deleted == 0) {
            return; // 未点赞
        }

        comment.setLikeCount(Math.max(comment.getLikeCount() - 1, 0));
        commentMapper.updateById(comment);
        log.info("取消点赞: commentId={}, userId={}", commentId, userId);
    }

    private CommentVO toVO(CommentEntity entity, Map<UUID, String> nicknameMap, Set<UUID> likedIds) {
        return CommentVO.builder()
                .id(entity.getId())
                .landmarkId(entity.getLandmarkId())
                .userId(entity.getUserId())
                .nickname(nicknameMap.getOrDefault(entity.getUserId(), "未知用户"))
                .parentId(entity.getParentId())
                .content(entity.getContent())
                .likeCount(entity.getLikeCount())
                .isLiked(likedIds.contains(entity.getId()))
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
