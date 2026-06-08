package com.echocampus.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.echocampus.auth.dto.LoginRequest;
import com.echocampus.auth.dto.RegisterRequest;
import com.echocampus.auth.dto.SendCodeRequest;
import com.echocampus.auth.service.AuthService;
import com.echocampus.auth.vo.LoginVO;
import com.echocampus.user.entity.UserEntity;
import com.echocampus.user.mapper.UserMapper;
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

@Service
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final JavaMailSender mailSender;
    private final Map<String, CodeEntry> codeStore = new ConcurrentHashMap<>();
    private final Map<String, Long> lastSendTime = new ConcurrentHashMap<>();
    private final Map<String, Integer> dailySendCount = new ConcurrentHashMap<>();
    private final Map<String, String> dailySendDate = new ConcurrentHashMap<>();

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final int SEND_INTERVAL_SECONDS = 60;
    private static final int DAILY_LIMIT = 5;//每日限制5次
    private static final int CODE_EXPIRE_SECONDS = 300;//验证码5分钟过期

    private record CodeEntry(String code, long createdAt) {}

    public AuthServiceImpl(UserMapper userMapper, JavaMailSender mailSender) {
        this.userMapper = userMapper;
        this.mailSender = mailSender;
    }

    @Override
    public void sendCode(SendCodeRequest request) {
        // 1. 校验邮箱格式
        if (request.getEmail() == null || !EMAIL_PATTERN.matcher(request.getEmail()).matches()) {
            throw new BusinessException(EMAIL_FORMAT_ERROR);//"邮箱格式无效"
        }

        // 2. 检查发送频率（60秒限制）
        Long lastTime = lastSendTime.get(request.getEmail());
        long now = System.currentTimeMillis();
        if (lastTime != null && (now - lastTime) < SEND_INTERVAL_SECONDS * 1000L) {
            throw new BusinessException(EMAIL_SEND_FREQUENTLY);// "发送频率过高，请稍后再试"
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
            throw new BusinessException(EMAIL_SEND_THRESHOLD_EXCEEDED);//发送次数已达上限
        }

        // 4. 生成验证码
        String code = String.format("%06d", new SecureRandom().nextInt(1000000));
        codeStore.put(request.getEmail(), new CodeEntry(code, System.currentTimeMillis()));
        lastSendTime.put(request.getEmail(), now);
        dailySendCount.put(request.getEmail(), count + 1);
        sendMail(request.getEmail(), "EchoCampus 验证码", "您的验证码是：" + code + "，5分钟内有效。");
    }

    @Override
    public LoginVO register(RegisterRequest request) {
        // 1. 校验验证码
        CodeEntry entry = codeStore.get(request.getEmail());
        if (entry == null) {
            throw new BusinessException(EMAIL_VERIFICATION_ERROR);// "请先获取验证码"
        }
        if (System.currentTimeMillis() - entry.createdAt() > CODE_EXPIRE_SECONDS * 1000L) {
            codeStore.remove(request.getEmail());
            throw new BusinessException(EMAIL_VERIFICATION_EXPIRED );//"验证码已过期"
        }
        if (!entry.code().equals(request.getCode())) {
            throw new BusinessException(EMAIL_VERIFICATION_ERROR);//"验证码错误"
        }

        // 2. 检查邮箱是否已注册
        Long count = userMapper.selectCount(
                new LambdaQueryWrapper<UserEntity>()
                        .eq(UserEntity::getEmail, request.getEmail()));
        if (count > 0) {
            throw new BusinessException(USER_ALREADY_EXISTS);//"该邮箱已注册"
        }

        // 3. 检查昵称
        if (request.getNickname() == null || request.getNickname().isBlank()) {
            throw new BusinessException(NICKNAME_IS_NULL);// "昵称不能为空"
        }

        if (request.getNickname().length() > 20) {
            throw new BusinessException(NICKNAME_LENGTH_ERROR);// "昵称长度不符合要求"
        }

        if (!request.getNickname().matches("^[a-zA-Z0-9_]+$")) {
            throw new BusinessException(NICKNAME_ILLEGAL_CHARACTERS);//"昵称包含非法字符"
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
            throw new BusinessException(USER_NOT_FOUND);//"邮箱未注册"
        }

        // 2. 校验密码
        String hashed = hashPassword(request.getPassword());
        if (!hashed.equals(user.getPasswordHash())) {
            throw new BusinessException(USER_PASSWORD_ERROR);//"密码错误"
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

    private void sendMail(String to, String subject, String text) {
        SimpleMailMessage message = new SimpleMailMessage();
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
