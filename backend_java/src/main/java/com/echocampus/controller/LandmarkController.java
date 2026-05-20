package com.echocampus.controller;

import com.echocampus.pojo.LandmarkDetail;
import com.echocampus.pojo.LandmarkPageItem;
import com.echocampus.pojo.PageResponse;
import com.echocampus.pojo.Result;
import com.echocampus.service.LandmarkService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/landmarks")
public class LandmarkController {
	private final LandmarkService landmarkService;

	public LandmarkController(LandmarkService landmarkService) {
		this.landmarkService = landmarkService;
	}

	@GetMapping("/page")
	public Result<PageResponse<LandmarkPageItem>> getLandmarkPage(
			@RequestParam(defaultValue = "1") int pageNum,
			@RequestParam(defaultValue = "10") int pageSize,
			@RequestParam(required = false) String category,
			@RequestParam(required = false) String keyword,
			@RequestParam(defaultValue = "false") boolean sortByScore
	) {
		int safePageNum = Math.max(pageNum, 1);
		int safePageSize = Math.min(Math.max(pageSize, 1), 50);
		PageResponse<LandmarkPageItem> data = landmarkService.getLandmarkPage(
				safePageNum,
				safePageSize,
				category,
				keyword,
				sortByScore
		);
		return Result.success(data);
	}

	@GetMapping("/{id}")
	public Result<LandmarkDetail> getLandmarkDetail(@PathVariable String id) {
		LandmarkDetail detail = landmarkService.getLandmarkDetail(id);
		if (detail == null) {
			return Result.failure(404, "not found");
		}
		return Result.success(detail);
	}
}
