package com.echocampus.admin.initializer;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.echocampus.admin.config.AdminProperties;
import com.echocampus.admin.entity.AdminEntity;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;
import com.echocampus.admin.mapper.AdminMapper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.UUID;

@Component
public class AdminInitializer {
    private final AdminProperties adminProperties;
    private final AdminMapper adminMapper;

    public AdminInitializer(AdminProperties adminProperties, AdminMapper adminMapper) {
        this.adminProperties = adminProperties;
        this.adminMapper = adminMapper;
    }

    @PostConstruct
    public void init() {
        if (adminProperties.getEmail().isEmpty() || adminProperties.getPassword().isEmpty() || adminProperties.getUsername().isEmpty()) {
            throw new RuntimeException("请配置管理员信息");
        }
        if (hasSuperAdmin()) {
            return;
        }
        adminMapper.insert(new AdminEntity()
                .setId(UUID.randomUUID())
                .setUsername(adminProperties.getUsername())
                .setPasswordHash(hashPassword(adminProperties.getPassword()))
                .setEmail(adminProperties.getEmail())
                .setIsSuper(true)
                .setCreatedAt(OffsetDateTime.now())
                .setUpdatedAt(OffsetDateTime.now()));
    }

    private boolean hasSuperAdmin() {
        LambdaQueryWrapper<AdminEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AdminEntity::getIsSuper, true);
        return adminMapper.selectCount(wrapper) > 0;
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
