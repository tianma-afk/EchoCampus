package com.echocampus.service.admin;

import com.echocampus.dto.UniversityCreateRequest;

import java.util.UUID;

public interface UniversityAdminService {
    UUID createUniversity(UniversityCreateRequest request);
}
