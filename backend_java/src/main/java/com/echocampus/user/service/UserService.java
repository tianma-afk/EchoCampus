package com.echocampus.user.service;

import com.echocampus.user.vo.UserProfileVO;

public interface UserService {
    UserProfileVO getProfile(String userId);
}
