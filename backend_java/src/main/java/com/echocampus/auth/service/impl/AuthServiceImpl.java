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

import com.echocampus.shared.exception.BusinessException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final Map<String, String> codeStore = new ConcurrentHashMap<>();
    private final Map<String, Long> lastSendTime = new ConcurrentHashMap<>();
    private final Map<String, Integer> dailySendCount = new ConcurrentHashMap<>();
    private final Map<String, String> dailySendDate = new ConcurrentHashMap<>();

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final int SEND_INTERVAL_SECONDS = 60;
    private static final int DAILY_LIMIT = 5;

    public AuthServiceImpl(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    public void sendCode(SendCodeRequest request) {
        // 1. 校验邮箱格式
        if (request.getEmail() == null || !EMAIL_PATTERN.matcher(request.getEmail()).matches()) {
            throw new BusinessException(40001, "邮箱格式无效");
        }

        // 2. 检查发送频率（60秒限制）
        Long lastTime = lastSendTime.get(request.getEmail());
        long now = System.currentTimeMillis();
        if (lastTime != null && (now - lastTime) < SEND_INTERVAL_SECONDS * 1000L) {
            throw new BusinessException(42901, "发送频率过高，请稍后再试");
        }

        // 3. 检查每日次数
        String today = LocalDate.now().toString();
        String storedDate = dailySendDate.get(request.getEmail());
        if (!today.equals(storedDate)) {
            dailySendDate.put(request.getEmail(), today);
            dailySendCount.put(request.getEmail(), 0);
        }
        int count = dailySendCount.getOrDefault(request.getEmail(), 0);
        if (count >= DAILY_LIMIT) {
            throw new BusinessException(42902, "今日发送次数已达上限");
        }

        // 4. 生成验证码
        String code = String.format("%06d", new SecureRandom().nextInt(1000000));
        codeStore.put(request.getEmail(), code);
        lastSendTime.put(request.getEmail(), now);
        dailySendCount.put(request.getEmail(), count + 1);
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
