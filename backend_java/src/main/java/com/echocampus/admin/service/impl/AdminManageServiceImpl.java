package com.echocampus.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.echocampus.admin.dto.AdminCreateRequest;
import com.echocampus.admin.dto.AdminUpdateRequest;
import com.echocampus.admin.entity.AdminEntity;
import com.echocampus.admin.mapper.AdminMapper;
import com.echocampus.admin.service.AdminManageService;
import com.echocampus.admin.vo.AdminVO;
import com.echocampus.shared.exception.BusinessException;
import com.echocampus.shared.exception.ErrorCode;
import com.echocampus.shared.util.PasswordUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.UUID;

@Slf4j
@Service
public class AdminManageServiceImpl implements AdminManageService {

    private final AdminMapper adminMapper;

    public AdminManageServiceImpl(AdminMapper adminMapper) {
        this.adminMapper = adminMapper;
    }

    @Override
    public Page<AdminVO> listAdmins(int page, int pageSize, String keyword) {
        LambdaQueryWrapper<AdminEntity> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w
                    .like(AdminEntity::getUsername, keyword)
                    .or()
                    .like(AdminEntity::getEmail, keyword));
        }
        wrapper.orderByAsc(AdminEntity::getCreatedAt);

        Page<AdminEntity> entityPage = adminMapper.selectPage(
                new Page<>(page, pageSize), wrapper);

        Page<AdminVO> voPage = new Page<>(page, pageSize, entityPage.getTotal());
        voPage.setRecords(entityPage.getRecords().stream()
                .map(this::toVO)
                .toList());
        return voPage;
    }

    @Override
    public AdminVO getAdmin(UUID id) {
        AdminEntity admin = adminMapper.selectById(id);
        if (admin == null) {
            throw new BusinessException(ErrorCode.ADMIN_NOT_FOUND);
        }
        return toVO(admin);
    }

    @Override
    public UUID createAdmin(AdminCreateRequest request) {
        if (adminMapper.selectCount(
                new LambdaQueryWrapper<AdminEntity>()
                        .eq(AdminEntity::getEmail, request.getEmail())) > 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "邮箱已存在");
        }

        AdminEntity admin = new AdminEntity()
                .setId(UUID.randomUUID())
                .setUsername(request.getUsername())
                .setPasswordHash(PasswordUtil.hashPassword(request.getPassword()))
                .setEmail(request.getEmail())
                .setIsSuper(false)
                .setCreatedAt(OffsetDateTime.now())
                .setUpdatedAt(OffsetDateTime.now());
        adminMapper.insert(admin);

        log.info("[管理员管理] 创建成功 -> adminId={}, username={}, isSuper={}",
                admin.getId(), admin.getUsername(), admin.getIsSuper());
        return admin.getId();
    }

    @Override
    public void updateAdmin(UUID id, AdminUpdateRequest request) {
        AdminEntity admin = adminMapper.selectById(id);
        if (admin == null || Boolean.TRUE.equals(admin.getIsSuper())) {
            throw new BusinessException(ErrorCode.ADMIN_NOT_FOUND);
        }

        if (request.getUsername() != null) {
            admin.setUsername(request.getUsername());
        }

        if (request.getPassword() != null) {
            admin.setPasswordHash(PasswordUtil.hashPassword(request.getPassword()));
        }

        if (request.getEmail() != null) {
            long count = adminMapper.selectCount(
                    new LambdaQueryWrapper<AdminEntity>()
                            .eq(AdminEntity::getEmail, request.getEmail())
                            .ne(AdminEntity::getId, id));
            if (count > 0) {
                throw new BusinessException(ErrorCode.CONFLICT, "邮箱已存在");
            }
            admin.setEmail(request.getEmail());
        }

        admin.setUpdatedAt(OffsetDateTime.now());
        adminMapper.updateById(admin);

        log.info("[管理员管理] 更新成功 -> adminId={}, username={}", admin.getId(), admin.getUsername());
    }

    @Override
    public void deleteAdmin(UUID id, String operatorId) {

        AdminEntity admin = adminMapper.selectById(id);
        if (admin == null) {
            throw new BusinessException(ErrorCode.ADMIN_NOT_FOUND);
        }

        if (Boolean.TRUE.equals(admin.getIsSuper())) {
            throw new BusinessException(ErrorCode.CONFLICT, "不能删除超级管理员");
        }

        adminMapper.deleteById(id);
        log.info("[管理员管理] 删除成功 -> adminId={}, username={}, operatorId={}",
                id, admin.getUsername(), operatorId);
    }

    private AdminVO toVO(AdminEntity entity) {
        return AdminVO.builder()
                .id(entity.getId())
                .username(entity.getUsername())
                .email(entity.getEmail())
                .isSuper(entity.getIsSuper())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
