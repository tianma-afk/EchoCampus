package com.echocampus.campus.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.echocampus.campus.dto.CampusCreateRequest;
import com.echocampus.campus.dto.CampusUpdateRequest;
import com.echocampus.campus.vo.CampusVO;

import java.util.List;
import java.util.UUID;

public interface CampusAdminService {
    UUID createCampus(CampusCreateRequest request);

    Page<CampusVO> listCampuses(int page, int pageSize, UUID universityId);

    CampusVO getCampus(UUID id);

    void updateCampus(UUID id, CampusUpdateRequest request);

    void deleteCampus(UUID id);

    List<CampusVO> searchCampuses(String keyword, UUID universityId);
}
