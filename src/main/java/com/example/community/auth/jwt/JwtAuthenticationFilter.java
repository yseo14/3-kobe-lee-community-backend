package com.example.community.auth.jwt;

import com.example.community.auth.jwt.exception.InvalidTokenException;
import com.example.community.global.response.code.ErrorReasonDto;
import com.example.community.global.response.code.status.ErrorStatus;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.GenericFilterBean;
import org.springframework.web.util.UrlPathHelper;

@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends GenericFilterBean {

    private final JwtUtils jwtUtils;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        // 타입 캐스팅
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // 현재 요청의 경로를 얻어온다.
        String path = new UrlPathHelper().getPathWithinApplication(httpRequest);
        String method = httpRequest.getMethod();

        // 경로 매칭을 위한 AntPathMatcher 사용
        AntPathMatcher pathMatcher = new AntPathMatcher();
        log.info("Request URI = {}", httpRequest.getRequestURI());
        log.info("Resolved path = {}", path);

        // 인증이 필요하지 않은 경로에 대해 필터를 건너뛴다.
        if ((pathMatcher.match("/auth", path) && "POST".equalsIgnoreCase(method)) ||  // 로그인만 허용
                (pathMatcher.match("/auth/refresh", path) && "POST".equalsIgnoreCase(method)) || // 토큰 재발급 허용
                (pathMatcher.match("/member", path) && "POST".equalsIgnoreCase(method)) || // 회원가입만 허용
                (pathMatcher.match("/member/email", path) && "GET".equalsIgnoreCase(method)) ||
                (pathMatcher.match("/member/nickname", path) && "GET".equalsIgnoreCase(method)) ||
                (pathMatcher.match("/terms", path) && "GET".equalsIgnoreCase(method)) ||
                (pathMatcher.match("/privacy", path) && "GET".equalsIgnoreCase(method)) ||
                (pathMatcher.match("/actuator/health", path) && "GET".equalsIgnoreCase(method))
        ) {

            log.info("🔹 Skipping JWT filter for path: {}", path);
            chain.doFilter(request, response);
            log.info("🔸 Chain executed successfully for path: {}", path);
            return;
        }

        // 요청으로부터 JWT 토큰을 추출
        String token = jwtUtils.resolveToken(httpRequest); // acccess token 검사

        log.info("token: {}", token);

        if (token == null) {
            ErrorReasonDto reason = ErrorStatus.EMPTY_TOKEN.getReasonHttpStatus();

            log.info("Access token is missing or expired");

            httpResponse.setStatus(reason.getHttpStatus().value());
            httpResponse.setContentType("application/json;charset=UTF-8");

            String jsonResponse = String.format(
                    "{\"isSuccess\": false, \"code\": \"%s\", \"message\": \"%s\"}",
                    reason.getCode(),
                    reason.getMessage()
            );

            httpResponse.getWriter().write(jsonResponse);
            httpResponse.getWriter().flush();

            return; // 체인 중단
        }

        try {
            jwtUtils.validateToken(token);

            Authentication authentication = jwtUtils.getAuthentication(token);
            SecurityContextHolder.getContext().setAuthentication(authentication);

        } catch (InvalidTokenException e) {

            ErrorReasonDto reason = e.getErrorReasonHttpStatus();

            log.info("Token validation failed: {}", reason.getMessage());

            httpResponse.setStatus(reason.getHttpStatus().value());
            httpResponse.setContentType("application/json;charset=UTF-8");

            String jsonResponse = String.format(
                    "{\"isSuccess\": false, \"code\": \"%s\", \"message\": \"%s\"}",
                    reason.getCode(),
                    reason.getMessage()
            );

            httpResponse.getWriter().write(jsonResponse);
            httpResponse.getWriter().flush();

            return; // 체인 중단
        }

        // 필터 체인의 다음 단계로 진행
        chain.doFilter(httpRequest, httpResponse);
    }


}