package com.example.community.auth.jwt;

import com.example.community.auth.jwt.exception.InvalidTokenException;
import com.example.community.global.response.code.ErrorReasonDto;
import com.example.community.global.response.code.status.ErrorStatus;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;

    private static final List<ExcludedRoute> EXCLUDED_ROUTES = List.of(
            new ExcludedRoute("/auth", "POST"),
            new ExcludedRoute("/auth/refresh", "POST"),
            new ExcludedRoute("/member/email", "GET"),
            new ExcludedRoute("/member/nickname", "GET"),
            new ExcludedRoute("/terms", "GET")
    );

    private final AntPathMatcher matcher = new AntPathMatcher();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod();

        if ("OPTIONS".equalsIgnoreCase(method)) {
            return true;
        }

        return EXCLUDED_ROUTES.stream().anyMatch(route ->
                matcher.match(route.path(), path) && method.equalsIgnoreCase(route.method())
        );
    }


    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String token = extractToken(request);
        if (token.isEmpty()) {
            sendErrorResponse(response, ErrorStatus.EMPTY_TOKEN.getReason());
            return;
        }

        try {
            jwtUtils.validateToken(token);
            Claims claims = jwtUtils.parseClaims(token);

            // 인증 성공, 사용자 정보 요청 속성에 저장
            request.setAttribute("memberId", Long.valueOf(claims.getSubject()));
            request.setAttribute("roles", claims.get("auth"));

        } catch (InvalidTokenException e) {
            sendErrorResponse(response, e.getErrorReasonHttpStatus());
            return;
        } catch (Exception e) {
            sendErrorResponse(response, ErrorStatus.INVALID_TOKEN.getReasonHttpStatus());
            return;
        }

        // 다음 필터로 진행
        filterChain.doFilter(request, response);

    }

    // 토큰 추출
    private String extractToken(HttpServletRequest request) {
        return jwtUtils.resolveToken(request);
    }

    // 에러 응답 통일
    private void sendErrorResponse(HttpServletResponse response, ErrorReasonDto reason) throws IOException {
        response.setHeader("Access-Control-Allow-Origin", "http://localhost:3000");
        response.setHeader("Access-Control-Allow-Credentials", "true");
        response.setHeader("Access-Control-Allow-Headers", "Authorization, Content-Type");
        response.setHeader("Access-Control-Allow-Methods", "GET, POST, PATCH, DELETE, OPTIONS");

        response.setStatus(reason.getHttpStatus().value());
        response.setContentType("application/json;charset=UTF-8");

        String json = String.format(
                "{\"isSuccess\": false, \"code\": \"%s\", \"message\": \"%s\"}",
                reason.getCode(), reason.getMessage()
        );

        response.getWriter().write(json);
        response.getWriter().flush();
    }

    private record ExcludedRoute(String path, String method) {
    }
}
