package com.echocampus.auth.service;

import com.echocampus.auth.dto.AdminLoginRequest;
import com.echocampus.auth.vo.LoginVO;

public interface AdminAuthService {
    LoginVO login(AdminLoginRequest request);
}
