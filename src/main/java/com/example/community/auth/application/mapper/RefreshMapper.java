package com.example.community.auth.application.mapper;

import com.example.community.auth.api.dto.RefreshResponse;
import com.example.community.auth.jwt.JwtToken;

public class RefreshMapper {
    public static RefreshResponse toRefreshResponse(JwtToken jwtToken) {
        return new RefreshResponse(jwtToken.getAccessToken(), jwtToken.getRefreshToken());
    }
}
