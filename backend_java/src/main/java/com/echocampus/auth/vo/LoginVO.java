package com.echocampus.auth.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginVO {

    private UUID userId;

    private String nickname;

    private String email;

    private String accessToken;

    private String refreshToken;

    private long expiresIn;
}
