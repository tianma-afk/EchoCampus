package com.echocampus.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.echocampus.shared.exception.BusinessException;
import com.echocampus.shared.exception.ErrorCode;
import com.echocampus.user.dto.UserUpdateRequest;
import com.echocampus.user.entity.UserEntity;
import com.echocampus.user.mapper.UserMapper;
import com.echocampus.user.service.UserService;
import com.echocampus.user.vo.UserProfileVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final Map<String, Integer> dailyNicknameChangeCount = new ConcurrentHashMap<>();
    private final Map<String, String> dailyNicknameChangeDate = new ConcurrentHashMap<>();
    private static final int DAILY_NICKNAME_CHANGE_LIMIT = 2;

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
                .remainingNicknameChanges(getRemainingChanges(userId))
                .build();
    }

    private int getRemainingChanges(String userId) {
        String today = LocalDate.now().toString();
        String storedDate = dailyNicknameChangeDate.get(userId);
        if (!today.equals(storedDate)) {
            return DAILY_NICKNAME_CHANGE_LIMIT;
        }
        int count = dailyNicknameChangeCount.getOrDefault(userId, 0);
        return Math.max(0, DAILY_NICKNAME_CHANGE_LIMIT - count);
    }

    @Override
    public UserProfileVO updateProfile(String userId, UserUpdateRequest request) {
        UserEntity user = userMapper.selectOne(
                new LambdaQueryWrapper<UserEntity>()
                        .eq(UserEntity::getId, UUID.fromString(userId)));
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        String today = LocalDate.now().toString();
        String storedDate = dailyNicknameChangeDate.get(userId);
        if (!today.equals(storedDate)) {
            dailyNicknameChangeDate.put(userId, today);
            dailyNicknameChangeCount.put(userId, 0);
        }
        int count = dailyNicknameChangeCount.getOrDefault(userId, 0);
        if (count >= DAILY_NICKNAME_CHANGE_LIMIT) {
            throw new BusinessException(ErrorCode.NICKNAME_CHANGE_LIMIT);
        }

        String nickname = request.getNickname();
        if (nickname == null || nickname.isBlank()) {
            throw new BusinessException(ErrorCode.NICKNAME_IS_NULL);
        }
        if (nickname.length() < 1 || nickname.length() > 20) {
            throw new BusinessException(ErrorCode.NICKNAME_LENGTH_ERROR);
        }
        if (!nickname.matches("^[\\u4e00-\\u9fa5a-zA-Z0-9_\\-]+$")) {
            throw new BusinessException(ErrorCode.NICKNAME_ILLEGAL_CHARACTERS);
        }
        user.setNickname(nickname);
        userMapper.updateById(user);
        dailyNicknameChangeCount.put(userId, count + 1);
        return UserProfileVO.builder()
                .id(user.getId().toString())
                .nickname(user.getNickname())
                .email(user.getEmail())
                .remainingNicknameChanges(Math.max(0, DAILY_NICKNAME_CHANGE_LIMIT - (count + 1)))
                .build();
    }
}
