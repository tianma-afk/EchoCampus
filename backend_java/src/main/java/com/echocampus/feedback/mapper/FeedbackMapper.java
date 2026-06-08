package com.echocampus.feedback.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.echocampus.feedback.entity.FeedbackEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface FeedbackMapper extends BaseMapper<FeedbackEntity> {
}
