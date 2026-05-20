package com.echocampus.mapper;

import com.echocampus.entity.FloorEntity;
import com.echocampus.entity.LandmarkDetailEntity;
import com.echocampus.entity.LandmarkPageEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface LandmarkMapper {
	long countLandmarks(@Param("category") String category, @Param("keyword") String keyword);

	List<LandmarkPageEntity> selectLandmarks(
			@Param("offset") int offset,
			@Param("limit") int limit,
			@Param("category") String category,
			@Param("keyword") String keyword,
			@Param("sortByScore") boolean sortByScore
	);

	LandmarkDetailEntity selectLandmarkDetail(@Param("id") String id);

	List<String> selectImagesByLandmarkId(@Param("id") String id);

	List<FloorEntity> selectFloorsByLandmarkId(@Param("id") String id);
}
