package com.echocampus.service.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.echocampus.dto.UniversityCreateRequest;
import com.echocampus.dto.UniversityUpdateRequest;
import com.echocampus.vo.UniversityVO;

import java.util.List;
import java.util.UUID;

public interface UniversityAdminService {
    UUID createUniversity(UniversityCreateRequest request);

    Page<UniversityVO> listUniversities(int page, int pageSize);

    UniversityVO getUniversity(UUID id);

    void updateUniversity(UUID id, UniversityUpdateRequest request);

    void deleteUniversity(UUID id);

    Page<UniversityVO> searchUniversities(String keyword, int page, int pageSize);
}
