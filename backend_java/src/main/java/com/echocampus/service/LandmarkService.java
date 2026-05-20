package com.echocampus.service;

import com.echocampus.pojo.LandmarkDetail;
import com.echocampus.pojo.LandmarkPageItem;
import com.echocampus.pojo.PageResponse;

public interface LandmarkService {
	PageResponse<LandmarkPageItem> getLandmarkPage(
			int pageNum,
			int pageSize,
			String category,
			String keyword,
			boolean sortByScore
	);

	LandmarkDetail getLandmarkDetail(String id);
}
