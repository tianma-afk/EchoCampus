package com.echocampus.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.echocampus.auth.dto.LoginRequest;
import com.echocampus.auth.dto.RegisterRequest;
import com.echocampus.auth.dto.SendCodeRequest;
import com.echocampus.auth.service.AuthService;
import com.echocampus.auth.vo.LoginVO;
import com.echocampus.user.entity.UserEntity;
import com.echocampus.user.mapper.UserMapper;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final Map<String, String> codeStore = new ConcurrentHashMap<>();

    public AuthServiceImpl(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    public void sendCode(SendCodeRequest request) {
        String code = String.format("%06d", new SecureRandom().nextInt(1000000));
        codeStore.put(request.getEmail(), code);
        System.out.println("[验证码] 邮箱: " + request.getEmail() + " 验证码: " + code);
    }

    @Override
    public LoginVO register(RegisterRequest request) {
        // 1. 校验验证码
        String storedCode = codeStore.get(request.getEmail());
        if (storedCode == null || !storedCode.equals(request.getCode())) {
            throw new RuntimeException("验证码错误或已过期");
        }

        // 2. 检查邮箱是否已注册
        Long count = userMapper.selectCount(
                new LambdaQueryWrapper<UserEntity>()
                        .eq(UserEntity::getEmail, request.getEmail()));
        if (count > 0) {
            throw new RuntimeException("该邮箱已注册");
        }

        // 3. 创建用户
        UserEntity user = new UserEntity();
        user.setId(UUID.randomUUID());
        user.setEmail(request.getEmail());
        user.setNickname(request.getNickname());
        user.setPasswordHash(hashPassword(request.getPassword()));
        user.setCreatedAt(OffsetDateTime.now());
        userMapper.insert(user);

        // 4. 清除已使用的验证码
        codeStore.remove(request.getEmail());

        // 5. 返回登录信息
        return LoginVO.builder()
                .userId(user.getId())
                .nickname(user.getNickname())
                .email(user.getEmail())
                .accessToken("token-" + user.getId())
                .expiresIn(7200)
                .build();
    }

    @Override
    public LoginVO login(LoginRequest request) {
        // 1. 查用户
        UserEntity user = userMapper.selectOne(
                new LambdaQueryWrapper<UserEntity>()
                        .eq(UserEntity::getEmail, request.getEmail()));
        if (user == null) {
            throw new RuntimeException("邮箱未注册");
        }

        // 2. 校验密码
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new RuntimeException("密码不能为空");
        }
        String hashed = hashPassword(request.getPassword());
        if (!hashed.equals(user.getPasswordHash())) {
            throw new RuntimeException("密码错误");
        }

        // 3. 返回登录信息
        return LoginVO.builder()
                .userId(user.getId())
                .nickname(user.getNickname())
                .email(user.getEmail())
                .accessToken("token-" + user.getId())
                .expiresIn(7200)
                .build();
    }

    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("密码加密失败", e);
        }
    }
}
