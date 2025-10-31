package com.example.community.auth.application.service;

import com.example.community.auth.Exception.LoginFailedException;
import com.example.community.auth.api.dto.LoginRequest;
import com.example.community.auth.api.dto.LogoutResponse;
import com.example.community.auth.api.dto.SessionLoginResponse;
import com.example.community.auth.application.mapper.LogoutMapper;
import com.example.community.auth.application.mapper.SessionMapper;
import com.example.community.global.redis.RedisDao;
import com.example.community.member.domain.Member;
import com.example.community.member.exception.MemberNotFoundException;
import com.example.community.member.repository.MemberRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SessionAuthService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;
    private final RedisDao redisDao;
    private static final Duration SESSION_TTL = Duration.ofMinutes(30);

    public SessionLoginResponse login(LoginRequest request, HttpServletResponse response) {
        String email = request.email();
        String rawPassword = request.password();

        Member member = memberRepository.findByEmail(email)
                .orElseThrow(MemberNotFoundException::new);

        if (!passwordEncoder.matches(rawPassword, member.getPassword())) {
            throw new LoginFailedException();
        }

        // 세션 ID 생성
        String sessionId = UUID.randomUUID().toString();

        // 세션 데이터 구성
        Map<String, Object> sessionData = new HashMap<>();
        sessionData.put("memberId", member.getId());
        sessionData.put("nickname", member.getNickname());

        try {
            String json = objectMapper.writeValueAsString(sessionData);
            redisDao.setValues("session:" + sessionId, json, SESSION_TTL);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("세션 저장 실패", e);
        }

        // 쿠키 생성
        Cookie cookie = new Cookie("SESSION_ID", sessionId);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setMaxAge((int) SESSION_TTL.toSeconds());
        response.addCookie(cookie);

        return SessionMapper.toSessionLoginResponse(member);
    }

    public LogoutResponse logout(HttpServletRequest request, HttpServletResponse response) {
        // 세션 id 추출
        String sessionId = extractSessionId(request);

        if (sessionId == null) {
            return LogoutMapper.toLogoutResponse();
        }

        // Redis에서 저장된 세션 삭제
        redisDao.deleteValues("session:" + sessionId);

        // 클라이언트의 쿠키를 만료시킨다. 같은 이름의 쿠키를 발행해서 만료시켜야함.
        Cookie expiredCookie = new Cookie("SESSION_ID", null);
        expiredCookie.setPath("/");
        expiredCookie.setHttpOnly(true);
        expiredCookie.setSecure(true);
        expiredCookie.setMaxAge(0);
        response.addCookie(expiredCookie);

        return LogoutMapper.toLogoutResponse();
    }

    private String extractSessionId(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return null;
        }
        for (Cookie cookie : request.getCookies()) {
            if ("SESSION_ID".equalsIgnoreCase(cookie.getName())) {
                String value = cookie.getValue();
                if (value != null && !value.isBlank()) {
                    return value;
                }
            }
        }
        return null;
    }
}
