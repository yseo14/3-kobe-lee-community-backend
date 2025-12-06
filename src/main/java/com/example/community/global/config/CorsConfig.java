package com.example.community.global.config;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Slf4j
@Configuration
public class CorsConfig {

    @Value("${custom.cors.allowed-origins}")
    private String allowedOriginsString;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // 환경변수가 쉼표로 구분된 문자열이거나 단일 문자열일 수 있으므로 파싱
        List<String> allowedOrigins = parseAllowedOrigins(allowedOriginsString);

        log.info("CORS 설정 - 허용된 Origin: {}", allowedOrigins);
        log.info("CORS 설정 - 원본 환경변수 값: {}", allowedOriginsString);

        // setAllowCredentials(true)를 사용할 때는 setAllowedOrigins()를 사용해야 함
        // 패턴이 아닌 정확한 origin을 사용
        configuration.setAllowedOrigins(allowedOrigins);

        // 허용할 HTTP 메서드
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));

        // 허용할 헤더
        configuration.addAllowedHeader("*");

        // 인증 정보 포함 허용
        configuration.setAllowCredentials(true);

        // 응답 헤더 노출
        configuration.addExposedHeader("Authorization");
        configuration.addExposedHeader("Set-Cookie");

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    private List<String> parseAllowedOrigins(String originsString) {
        if (!StringUtils.hasText(originsString)) {
            return List.of("http://localhost:3000");
        }

        // 쉼표로 구분된 문자열을 리스트로 변환하고 공백 제거
        return Arrays.stream(originsString.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .collect(Collectors.toList());
    }
}