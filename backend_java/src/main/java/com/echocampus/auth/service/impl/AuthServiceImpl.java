package com.echocampus.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.echocampus.auth.dto.LoginRequest;
import com.echocampus.auth.dto.RegisterRequest;
import com.echocampus.auth.dto.SendCodeRequest;
import com.echocampus.auth.service.AuthService;
import com.echocampus.auth.vo.LoginVO;
import com.echocampus.user.entity.UserEntity;
import com.echocampus.user.mapper.UserMapper;
import com.echocampus.shared.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import com.echocampus.shared.exception.BusinessException;
import static com.echocampus.shared.exception.ErrorCode.*;

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

@Slf4j
@Service
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final JavaMailSender mailSender;
    private final JwtUtil jwtUtil;
    private final Map<String, CodeEntry> codeStore = new ConcurrentHashMap<>();
    private final Map<String, Long> lastSendTime = new ConcurrentHashMap<>();
    private final Map<String, Integer> dailySendCount = new ConcurrentHashMap<>();
    private final Map<String, String> dailySendDate = new ConcurrentHashMap<>();

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final int SEND_INTERVAL_SECONDS = 60;
    private static final int DAILY_LIMIT = 5;
    private static final int CODE_EXPIRE_SECONDS = 300;

    private record CodeEntry(String code, long createdAt) {}

    public AuthServiceImpl(UserMapper userMapper, JavaMailSender mailSender, JwtUtil jwtUtil) {
        this.userMapper = userMapper;
        this.mailSender = mailSender;
        this.jwtUtil = jwtUtil;
    }

    @Override
    public void sendCode(SendCodeRequest request) {
        log.info("[sendCode] 开始处理 -> email={}", request.getEmail());

        if (request.getEmail() == null || !EMAIL_PATTERN.matcher(request.getEmail()).matches()) {
            log.warn("[sendCode] 邮箱格式无效 -> email={}", request.getEmail());
            throw new BusinessException(EMAIL_FORMAT_ERROR);
        }

        Long lastTime = lastSendTime.get(request.getEmail());
        long now = System.currentTimeMillis();
        if (lastTime != null && (now - lastTime) < SEND_INTERVAL_SECONDS * 1000L) {
            long remain = SEND_INTERVAL_SECONDS - (now - lastTime) / 1000;
            log.warn("[sendCode] 发送太频繁 -> email={}, 剩余{}秒", request.getEmail(), remain);
            throw new BusinessException(EMAIL_SEND_FREQUENTLY);
        }

        String today = LocalDate.now().toString();
        String storedDate = dailySendDate.get(request.getEmail());
        if (!today.equals(storedDate)) {
            dailySendDate.put(request.getEmail(), today);
            dailySendCount.put(request.getEmail(), 0);
            log.info("[sendCode] 新的一天，重置计数 -> email={}", request.getEmail());
        }
        int count = dailySendCount.getOrDefault(request.getEmail(), 0);
        if (count >= DAILY_LIMIT) {
            log.warn("[sendCode] 超过每日上限 -> email={}, 今日已发={}", request.getEmail(), count);
            throw new BusinessException(EMAIL_SEND_THRESHOLD_EXCEEDED);
        }

        String code = String.format("%06d", new SecureRandom().nextInt(1000000));
        codeStore.put(request.getEmail(), new CodeEntry(code, System.currentTimeMillis()));
        lastSendTime.put(request.getEmail(), now);
        dailySendCount.put(request.getEmail(), count + 1);
        log.info("[sendCode] 验证码已生成 -> email={}, code={}", request.getEmail(), code);
        sendMail(request.getEmail(), "EchoCampus 验证码", "您的验证码是：" + code + "，5分钟内有效。");
        log.info("[sendCode] 邮件已发送 -> email={}, 今日第{}封", request.getEmail(), count + 1);
    }

    @Override
    public LoginVO register(RegisterRequest request) {
        log.info("[register] 开始处理 -> email={}", request.getEmail());

        CodeEntry entry = codeStore.get(request.getEmail());
        if (entry == null) {
            log.warn("[register] 未找到验证码 -> email={}", request.getEmail());
            throw new BusinessException(EMAIL_VERIFICATION_ERROR);
        }
        if (System.currentTimeMillis() - entry.createdAt() > CODE_EXPIRE_SECONDS * 1000L) {
            codeStore.remove(request.getEmail());
            log.warn("[register] 验证码已过期 -> email={}", request.getEmail());
            throw new BusinessException(EMAIL_VERIFICATION_EXPIRED);
        }
        if (!entry.code().equals(request.getCode())) {
            log.warn("[register] 验证码不匹配 -> email={}, 期望={}, 收到={}", request.getEmail(), entry.code(), request.getCode());
            throw new BusinessException(EMAIL_VERIFICATION_ERROR);
        }

        UserEntity user = userMapper.selectOne(
                new LambdaQueryWrapper<UserEntity>()
                        .eq(UserEntity::getEmail, request.getEmail()));

        if (user == null) {
            log.info("[register] 新用户注册 -> email={}, nickname={}", request.getEmail(), request.getNickname());
            if (request.getPassword() == null || request.getPassword().isBlank()) {
                throw new BusinessException(VALIDATION_ERROR, "密码不能为空");
            }
            if (request.getNickname() == null || request.getNickname().isBlank()) {
                throw new BusinessException(NICKNAME_IS_NULL);
            }
            if (request.getNickname().length() > 20) {
                throw new BusinessException(NICKNAME_LENGTH_ERROR);
            }
            if (!request.getNickname().matches("^[a-zA-Z0-9_]+$")) {
                throw new BusinessException(NICKNAME_ILLEGAL_CHARACTERS);
            }

            user = new UserEntity();
            user.setId(UUID.randomUUID());
            user.setEmail(request.getEmail());
            user.setNickname(request.getNickname());
            user.setPasswordHash(hashPassword(request.getPassword()));
            user.setCreatedAt(OffsetDateTime.now());
            userMapper.insert(user);
            log.info("[register] 用户已创建 -> userId={}, email={}", user.getId(), user.getEmail());
        } else {
            log.info("[register] 已有用户登录 -> userId={}, email={}", user.getId(), request.getEmail());
        }

        codeStore.remove(request.getEmail());

        String jwt = jwtUtil.generateToken(user.getId().toString());
        log.info("[register] JWT已生成 -> userId={}, token前8位={}...", user.getId(), jwt.substring(0, 8));
        return LoginVO.builder()
                .accessToken(jwt)
                .tokenType("Bearer")
                .expiresIn(jwtUtil.getExpiration())
                .nickname(user.getNickname())
                .build();
    }

    @Override
    public LoginVO login(LoginRequest request) {
        log.info("[login] 开始处理 -> email={}", request.getEmail());

        UserEntity user = userMapper.selectOne(
                new LambdaQueryWrapper<UserEntity>()
                        .eq(UserEntity::getEmail, request.getEmail()));
        if (user == null) {
            log.warn("[login] 用户不存在 -> email={}", request.getEmail());
            throw new BusinessException(USER_NOT_FOUND);
        }

        String hashed = hashPassword(request.getPassword());
        if (!hashed.equals(user.getPasswordHash())) {
            log.warn("[login] 密码错误 -> email={}", request.getEmail());
            throw new BusinessException(USER_PASSWORD_ERROR);
        }

        log.info("[login] 登录成功 -> userId={}, email={}", user.getId(), user.getEmail());
        return LoginVO.builder()
                .accessToken(jwtUtil.generateToken(user.getId().toString()))
                .tokenType("Bearer")
                .expiresIn(jwtUtil.getExpiration())
                .nickname(user.getNickname())
                .build();
    }

    private void sendMail(String to, String subject, String text) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("3292513488@qq.com");
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        mailSender.send(message);
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
