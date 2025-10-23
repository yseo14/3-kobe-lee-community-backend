package com.example.community.global.config;

import java.util.Arrays;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CorsConfig {
    public static CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // 허용할 Origin 패턴 (정확히 일치하거나 패턴 매칭 허용)
        configuration.setAllowedOriginPatterns(Arrays.asList(
                "http://localhost:3000"
        ));

        // 허용할 HTTP 메서드
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PATCH", "DELETE"));

        // 허용할 헤더
        configuration.addAllowedHeader("*");

        // 인증 정보(쿠키 등) 포함 허용
        configuration.setAllowCredentials(true);

        // 응답 헤더로 노출할 헤더 (선택)
        configuration.addExposedHeader("Authorization");
        configuration.addExposedHeader("Set-Cookie");

        // 경로 매핑
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}