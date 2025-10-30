package com.example.community.global.config;

import com.example.community.auth.jwt.JwtAuthenticationFilter;
import com.example.community.auth.jwt.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final JwtUtils jwtUtils;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity httpSecurity) throws Exception {
        return httpSecurity
                .formLogin(AbstractHttpConfigurer::disable)  // 기본 로그인 폼 비활성화
                .httpBasic(AbstractHttpConfigurer::disable)  // 기본 HTTP Basic 인증 비활성화
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(CorsConfig.corsConfigurationSource()))
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll() // 모든 요청을 허용으로 변경
                )

//                //  시큐리티 제거 효과를 위한 인가 설정 제거
//                .authorizeHttpRequests(auth -> auth
//                        .requestMatchers(HttpMethod.POST, "/auth").permitAll()          // 로그인
//                        .requestMatchers(HttpMethod.POST, "/auth/refresh").permitAll()  //  토큰 재발급
//                        .requestMatchers(HttpMethod.POST, "/member").permitAll()        // 회원가입
//                        .requestMatchers(HttpMethod.GET, "/member/email").permitAll()   // 이메일 중복 확인
//                        .requestMatchers(HttpMethod.GET, "/member/nickname").permitAll()// 닉네임 중복 확인
//                        .requestMatchers(HttpMethod.GET, "/terms").permitAll()  // 이용약관 조회
//                        .requestMatchers(HttpMethod.GET, "/privacy").permitAll()  // 개인정보 조회
//                        .anyRequest().authenticated()
//                )
//                .addFilterBefore(new JwtAuthenticationFilter(jwtUtils), UsernamePasswordAuthenticationFilter.class)

                .build();

    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
