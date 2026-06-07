package com.echocampus.auth.service;

import com.echocampus.auth.dto.LoginRequest;
import com.echocampus.auth.dto.RegisterRequest;
import com.echocampus.auth.dto.SendCodeRequest;
import com.echocampus.auth.vo.LoginVO;

public interface AuthService {

    void sendCode(SendCodeRequest request);

    LoginVO register(RegisterRequest request);

    LoginVO login(LoginRequest request);
}
