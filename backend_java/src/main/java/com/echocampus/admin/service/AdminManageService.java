package com.echocampus.admin.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.echocampus.admin.dto.AdminCreateRequest;
import com.echocampus.admin.dto.AdminUpdateRequest;
import com.echocampus.admin.vo.AdminVO;

import java.util.UUID;

public interface AdminManageService {
    Page<AdminVO> listAdmins(int page, int pageSize, String keyword);
    AdminVO getAdmin(UUID id);
    UUID createAdmin(AdminCreateRequest request);
    void updateAdmin(UUID id, AdminUpdateRequest request);
    void deleteAdmin(UUID id, String operatorId);
}
