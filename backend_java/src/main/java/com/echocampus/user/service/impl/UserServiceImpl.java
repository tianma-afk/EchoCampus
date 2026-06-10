package com.echocampus.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.echocampus.shared.exception.BusinessException;
import com.echocampus.shared.exception.ErrorCode;
import com.echocampus.user.entity.UserEntity;
import com.echocampus.user.mapper.UserMapper;
import com.echocampus.user.service.UserService;
import com.echocampus.user.vo.UserProfileVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;

    public UserServiceImpl(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    public UserProfileVO getProfile(String userId) {
        UserEntity user = userMapper.selectOne(
                new LambdaQueryWrapper<UserEntity>()
                        .eq(UserEntity::getId, UUID.fromString(userId)));
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        return UserProfileVO.builder()
                .id(user.getId().toString())
                .nickname(user.getNickname())
                .email(user.getEmail())
                .build();
    }
}
