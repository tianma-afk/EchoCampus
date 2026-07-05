package com.echocampus.comment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.echocampus.comment.entity.CommentLikeEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CommentLikeMapper extends BaseMapper<CommentLikeEntity> {
}
