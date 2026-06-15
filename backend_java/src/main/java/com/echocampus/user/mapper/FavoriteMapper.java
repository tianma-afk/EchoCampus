package com.echocampus.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.echocampus.user.entity.Favorite;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface FavoriteMapper extends BaseMapper<Favorite> {
}
