package com.echocampus.user.service;

import com.echocampus.user.dto.UserUpdateRequest;
import com.echocampus.user.vo.UserProfileVO;

public interface UserService {
    UserProfileVO getProfile(String userId);

    UserProfileVO updateProfile(String userId, UserUpdateRequest request);
}
