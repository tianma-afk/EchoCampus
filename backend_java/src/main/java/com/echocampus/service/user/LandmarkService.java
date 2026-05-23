package com.echocampus.service.user;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.echocampus.dto.LandmarkDetailRequest;
import com.echocampus.dto.LandmarkListRequest;
import com.echocampus.vo.LandmarkDetailVO;
import com.echocampus.vo.LandmarkVO;

public interface LandmarkService {

    Page<LandmarkVO> getLandmarkList(LandmarkListRequest request);

    LandmarkDetailVO getLandmarkDetail(LandmarkDetailRequest request);
}
