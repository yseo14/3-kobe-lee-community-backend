package com.example.community.auth.api;

import com.example.community.auth.Exception.TokenNotFoundException;
import com.example.community.auth.api.dto.LoginRequest;
import com.example.community.auth.api.dto.LoginResponse;
import com.example.community.auth.api.dto.LogoutResponse;
import com.example.community.auth.api.dto.RefreshResponse;
import com.example.community.auth.application.service.AuthService;
import com.example.community.global.response.ApiResponse;
import com.example.community.global.response.code.status.SuccessStatus;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    @PostMapping
    public ApiResponse<LoginResponse> login(@RequestBody LoginRequest request,
                                            HttpServletResponse httpServletResponse) {
        LoginResponse loginResponse = authService.login(request);
        String refreshToken = loginResponse.refreshToken();
        ResponseCookie cookie = ResponseCookie.from("refresh_token", refreshToken)
                .httpOnly(true) //  클라이언트나 스크립트에서 조작하지 못하도록 함
                .secure(false)                // 운영환경(HTTPS)에서는 true
                .path("/")
                .maxAge(7 * 24 * 60 * 60)   // 7일
                .sameSite("Lax")
                .build();
        httpServletResponse.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        return ApiResponse.onSuccess(SuccessStatus.LOGIN_SUCCESS, loginResponse);
    }

    @DeleteMapping
    public ApiResponse<LogoutResponse> logout(HttpServletRequest request) {
        LogoutResponse response = authService.logout(request);
        return ApiResponse.onSuccess(SuccessStatus.LOGOUT_SUCCESS, response);
    }

    @PostMapping("/refresh")
    public ApiResponse<RefreshResponse> refresh(
            @CookieValue(value = "refresh_token", required = false) String refreshToken,
            HttpServletResponse httpServletResponse) {

        if (refreshToken == null) {
            throw new TokenNotFoundException();
        }

        RefreshResponse refreshResponse = authService.refresh(refreshToken);
        String newRefreshToken = refreshResponse.refreshToken();
        ResponseCookie cookie = ResponseCookie.from("refresh_token", newRefreshToken)
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(7 * 24 * 60 * 60)
                .sameSite("Lax")
                .build();
        httpServletResponse.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        return ApiResponse.onSuccess(SuccessStatus.REFRESH_SUCCESS, refreshResponse);
    }
}
