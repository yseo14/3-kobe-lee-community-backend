package com.example.community.auth.application.service;

import com.example.community.auth.Exception.LoginFailedException;
import com.example.community.auth.api.dto.LoginRequest;
import com.example.community.auth.api.dto.LoginResponse;
import com.example.community.auth.api.dto.LogoutResponse;
import com.example.community.auth.api.dto.RefreshResponse;
import com.example.community.auth.application.mapper.LoginMapper;
import com.example.community.auth.application.mapper.LogoutMapper;
import com.example.community.auth.application.mapper.RefreshMapper;
import com.example.community.auth.jwt.JwtToken;
import com.example.community.auth.jwt.JwtUtils;
import com.example.community.global.redis.RedisDao;
import com.example.community.member.domain.Member;
import com.example.community.member.exception.MemberNotFoundException;
import com.example.community.member.repository.MemberRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.time.Duration;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final RedisDao redisDao;


    /**
     * 로그인. 이메일과 비밀번호 검증 후 AccessToken과 RefreshToken 발급
     */
    @Override
    public LoginResponse login(LoginRequest request) {
        String email = request.email();
        String rawPassword = request.password();

        Member member = memberRepository.findByEmail(email)
                .orElseThrow(MemberNotFoundException::new);

        if (!passwordEncoder.matches(rawPassword, member.getPassword())) {
            throw new LoginFailedException();
        }

        List<String> roles = List.of("ROLE_USER");

        // JWT 토큰 발급 (Access + Refresh)
        JwtToken jwtToken = jwtUtils.generateToken(
                member.getId(),
                roles
        );

        return LoginMapper.toLoginResponse(member, jwtToken);
    }

    /**
     * 로그아웃: Redis에 저장된 RefreshToken 제거 + AccessToken 블랙리스트 등록
     */
    @Override
    public LogoutResponse logout(HttpServletRequest request) {
        String accessToken = jwtUtils.resolveToken(request);

        // 남은 만료 시간 계산
        long remainingTime = jwtUtils.getRemainingExpiration(accessToken);

        // memberId 추출
        String memberId = jwtUtils.getUserMemberIdFromToken(accessToken);

        // RefreshToken 제거
        jwtUtils.deleteRefreshToken(memberId);

        // AccessToken 블랙리스트 등록 (만료될 때까지 유지)
        redisDao.setValues(
                "blacklist:" + accessToken,
                "logout",
                Duration.ofMillis(remainingTime)
        );

        return LogoutMapper.toLogoutResponse();
    }

    /**
     * RefreshToken 검증 후 Access/Refresh 재발급
     */
    @Override
    public RefreshResponse refresh(String refreshToken) {
        // refresh token 유효성 검사
        jwtUtils.validateRefreshToken(refreshToken);

        // 토큰에서 사용자 식별자 추출
        String memberId = jwtUtils.getUserMemberIdFromToken(refreshToken);

        long remainingTtl = jwtUtils.getRemainingExpiration(refreshToken);

        // Redis에서 기존 RefreshToken 제거
        jwtUtils.deleteRefreshToken(memberId);

        Member member = memberRepository.findById(Long.parseLong(memberId))
                .orElseThrow(MemberNotFoundException::new);

        // 권한 재지정
        List<String> roles = List.of("ROLE_USER");

        // 새 JWT 발급
        JwtToken newToken = jwtUtils.generateToken(member.getId(), roles, remainingTtl);

        return RefreshMapper.toRefreshResponse(newToken);
    }
}
