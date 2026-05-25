package com.echocampus.service.admin;

import com.echocampus.dto.CampusCreateRequest;
import com.echocampus.vo.CampusVO;

import java.util.List;
import java.util.UUID;

public interface CampusAdminService {
    UUID createCampus(CampusCreateRequest request);

    List<CampusVO> searchCampuses(String keyword, UUID universityId);
}
