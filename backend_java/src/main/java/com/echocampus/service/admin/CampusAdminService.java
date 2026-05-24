package com.echocampus.service.admin;

import com.echocampus.dto.CampusCreateRequest;

import java.util.UUID;

public interface CampusAdminService {
    UUID createCampus(CampusCreateRequest request);
}
