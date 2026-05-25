package com.echocampus.service.admin;

import com.echocampus.dto.UniversityCreateRequest;
import com.echocampus.vo.UniversityVO;

import java.util.List;
import java.util.UUID;

public interface UniversityAdminService {
    UUID createUniversity(UniversityCreateRequest request);

    List<UniversityVO> searchUniversities(String keyword);
}
