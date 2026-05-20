package com.echocampus.service.impl;

import com.echocampus.entity.FloorEntity;
import com.echocampus.entity.LandmarkDetailEntity;
import com.echocampus.entity.LandmarkPageEntity;
import com.echocampus.mapper.LandmarkMapper;
import com.echocampus.pojo.FloorInfo;
import com.echocampus.pojo.LandmarkDetail;
import com.echocampus.pojo.LandmarkPageItem;
import com.echocampus.pojo.PageResponse;
import com.echocampus.service.LandmarkService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class LandmarkServiceImpl implements LandmarkService {
	private final LandmarkMapper landmarkMapper;

	public LandmarkServiceImpl(LandmarkMapper landmarkMapper) {
		this.landmarkMapper = landmarkMapper;
	}

	@Override
	public PageResponse<LandmarkPageItem> getLandmarkPage(int pageNum, int pageSize, String category, String keyword, boolean sortByScore) {
		int offset = (Math.max(pageNum, 1) - 1) * pageSize;
		long total = landmarkMapper.countLandmarks(category, keyword);
		List<LandmarkPageEntity> entities = landmarkMapper.selectLandmarks(offset, pageSize, category, keyword, sortByScore);
		List<LandmarkPageItem> items = new ArrayList<>();
		for (LandmarkPageEntity e : entities) {
			items.add(new LandmarkPageItem(
					e.getId(),
					e.getName(),
					e.getScore(),
					e.getCoverUrl(),
					e.getCheckInCount(),
					parseJsonArray(e.getTags()),
					e.getOpenTime(),
					e.getCategoryName()
			));
		}
		int pages = (int) ((total + pageSize - 1) / pageSize);
		return new PageResponse<>(total, pageNum, pageSize, pages, items);
	}

	@Override
	public LandmarkDetail getLandmarkDetail(String id) {
		LandmarkDetailEntity e = landmarkMapper.selectLandmarkDetail(id);
		if (e == null) return null;
		List<String> images = landmarkMapper.selectImagesByLandmarkId(id);
		List<FloorEntity> floorEntities = landmarkMapper.selectFloorsByLandmarkId(id);
		List<FloorInfo> floors = new ArrayList<>();
		if (floorEntities != null) {
			for (FloorEntity fe : floorEntities) {
				floors.add(new FloorInfo(fe.getFloorNumber(), fe.getFloorName(), parseJsonArray(fe.getTags())));
			}
		}
		return new LandmarkDetail(
				e.getId(),
				e.getName(),
				e.getScore(),
				images == null ? new ArrayList<>() : images,
				e.getCheckInCount(),
				parseJsonArray(e.getTags()),
				e.getOpenTime(),
				e.getCategoryName(),
				e.getBuiltTime(),
				e.getDescription(),
				e.getLocationDescription(),
				e.getCampusName(),
				e.getUniversityName(),
				floors
		);
	}

	private static List<String> parseJsonArray(String json) {
		List<String> out = new ArrayList<>();
		if (json == null) return out;
		String s = json.trim();
		if (s.length() == 0) return out;
		if (s.startsWith("[")) {
			s = s.substring(1);
		}
		if (s.endsWith("]")) {
			s = s.substring(0, s.length() - 1);
		}
		// split by comma, remove surrounding quotes and whitespace
		String[] parts = s.split(",");
		for (String p : parts) {
			String t = p.trim();
			if (t.startsWith("\"") && t.endsWith("\"")) {
				t = t.substring(1, t.length() - 1);
			}
			// unescape common sequences
			t = t.replace("\\\"", "\"").replace("\\\\", "\\");
			if (!t.isEmpty()) out.add(t);
		}
		return out;
	}
}
