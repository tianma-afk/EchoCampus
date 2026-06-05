package com.echocampus.landmark.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.echocampus.landmark.dto.LandmarkDetailRequest;
import com.echocampus.landmark.dto.LandmarkListRequest;
import com.echocampus.landmark.vo.LandmarkDetailVO;
import com.echocampus.landmark.vo.LandmarkVO;

public interface LandmarkService {

    Page<LandmarkVO> getLandmarkList(LandmarkListRequest request);

    LandmarkDetailVO getLandmarkDetail(LandmarkDetailRequest request);
}
