package com.echocampus.cleanup.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.echocampus.cleanup.entity.MilvusPendingDeleteEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MilvusPendingDeleteMapper extends BaseMapper<MilvusPendingDeleteEntity> {
}
