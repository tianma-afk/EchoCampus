package com.echocampus.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.echocampus.admin.entity.AdminEntity;
import com.echocampus.admin.mapper.AdminMapper;
import com.echocampus.auth.dto.AdminLoginRequest;
import com.echocampus.auth.service.AdminAuthService;
import com.echocampus.auth.vo.LoginVO;
import com.echocampus.shared.enums.RoleEnum;
import com.echocampus.shared.exception.BusinessException;
import com.echocampus.shared.exception.ErrorCode;
import com.echocampus.shared.util.JwtUtil;
import com.echocampus.shared.util.PasswordUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AdminAuthServiceImpl implements AdminAuthService {

    private final AdminMapper adminMapper;
    private final JwtUtil jwtUtil;

    public AdminAuthServiceImpl(AdminMapper adminMapper, JwtUtil jwtUtil) {
        this.adminMapper = adminMapper;
        this.jwtUtil = jwtUtil;
    }

    @Override
    public LoginVO login(AdminLoginRequest request) {
        log.info("[管理员登录] 开始处理 -> username={}", request.getUsername());

        AdminEntity admin = adminMapper.selectOne(
                new LambdaQueryWrapper<AdminEntity>()
                        .eq(AdminEntity::getUsername, request.getUsername()));
        if (admin == null) {
            log.warn("[管理员登录] 管理员不存在 -> username={}", request.getUsername());
            throw new BusinessException(ErrorCode.ADMIN_LOGIN_ERROR);
        }

        String hashed = PasswordUtil.hashPassword(request.getPassword());
        if (!hashed.equals(admin.getPasswordHash())) {
            log.warn("[管理员登录] 密码错误 -> username={}", request.getUsername());
            throw new BusinessException(ErrorCode.ADMIN_LOGIN_ERROR);
        }

        RoleEnum role = Boolean.TRUE.equals(admin.getIsSuper())
                ? RoleEnum.SUPER_ADMIN
                : RoleEnum.ADMIN;

        String token = jwtUtil.generateToken(admin.getId().toString(), role);

        log.info("[管理员登录] 登录成功 -> adminId={}, username={}, role={}",
                admin.getId(), admin.getUsername(), role.getValue());

        return LoginVO.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .expiresIn(jwtUtil.getExpiration())
                .nickname(admin.getUsername())
                .role(role.getValue())
                .adminId(admin.getId().toString())
                .build();
    }
}
