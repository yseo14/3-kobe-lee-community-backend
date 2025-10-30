package com.example.community.global.config;

import com.example.community.global.redis.RedisDao;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.Map;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class SessionAuthFilter extends OncePerRequestFilter {
    private final RedisDao redisDao;
    private final ObjectMapper objectMapper;

    // 필터 제외 경로 목록
    private static final String[] EXCLUDED_PATHS = {
            "/auth/v2"
    };

    // 필터 제외 경로 설정
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return Arrays.stream(EXCLUDED_PATHS).anyMatch(path::startsWith);
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        String sessionId = extractSessionId(request);

        if (sessionId == null) {
            sendUnauthorized(response, "세션 ID가 없습니다.");
            return;
        }

        // Redis에서 세션 데이터 조회
        Object sessionJson = redisDao.getValues("session:" + sessionId);

        if (sessionJson == null) {
            sendUnauthorized(response, "세션이 만료되었거나 존재하지 않습니다.");
            return;
        }

        // 세션 데이터 파싱
        Map<String, Object> sessionData = objectMapper.readValue(
                sessionJson.toString(), new TypeReference<>() {}
        );

        // 세션 데이터를 Request Attribute에 저장
        request.setAttribute("session", sessionData);

        //  활동 중이라면 세션을 늘려준다.
        redisDao.setValues("session:" + sessionId, (String) sessionJson, Duration.ofMinutes(30));

        // 다음 필터로 진행
        filterChain.doFilter(request, response);
    }

    private String extractSessionId(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return null;
        }

        for (Cookie cookie :  request.getCookies()) {
            if ("SESSION_ID".equalsIgnoreCase(cookie.getName())) {
                String value = cookie.getValue();
                if (value != null && !value.isBlank()) {
                    return value;
                }
            }
        }

        // 세션 쿠키 없음
        return null;
    }

    private void sendUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"error\": \"" + message + "\"}");
    }
}
