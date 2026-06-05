package com.echocampus.landmark.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.echocampus.landmark.entity.ImageEntity;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ImageMapper extends BaseMapper<ImageEntity> {
}
