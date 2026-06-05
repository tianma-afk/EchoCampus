package com.echocampus.algorithm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.echocampus.algorithm.entity.TaskEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TaskMapper extends BaseMapper<TaskEntity> {
}
