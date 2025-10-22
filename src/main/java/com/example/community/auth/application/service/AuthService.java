package com.example.community.auth.application.service;

import com.example.community.auth.api.dto.LoginRequest;
import com.example.community.auth.api.dto.LoginResponse;
import com.example.community.auth.api.dto.LogoutResponse;
import com.example.community.auth.api.dto.RefreshResponse;
import jakarta.servlet.http.HttpServletRequest;

public interface AuthService {
    LoginResponse login(LoginRequest request);

    LogoutResponse logout(HttpServletRequest request);

    RefreshResponse refresh(String refreshToken);
}
