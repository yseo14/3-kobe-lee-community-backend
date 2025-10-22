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
import com.example.community.global.response.code.status.ErrorStatus;
import com.example.community.member.domain.Member;
import com.example.community.member.exception.MemberNotFoundException;
import com.example.community.member.repository.MemberRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManagerBuilder authenticationManagerBuilder;
    private final JwtUtils jwtUtils;
    private final RedisDao redisDao;

    @Override
    public LoginResponse login(LoginRequest request) {
        String email = request.email();
        String rawPassword = request.password();
        Member member = memberRepository.findByEmail(email).orElseThrow(MemberNotFoundException::new);

        if (!passwordEncoder.matches(rawPassword, member.getPassword())) {
            throw new LoginFailedException();
        }

        //  Spring Security 인증용 토큰
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(email,
                rawPassword);

        Authentication authentication = authenticationManagerBuilder.getObject().authenticate(authenticationToken);
        JwtToken jwtToken = jwtUtils.generateToken(authentication);
        return LoginMapper.toLoginResponse(member, jwtToken);
    }

    @Override
    public LogoutResponse logout(HttpServletRequest request) {
        String accessToken = jwtUtils.resolveToken(request);
        long remainingTime = jwtUtils.getRemainingExpiration(accessToken);
        String memberId = jwtUtils.getUserNameFromToken(accessToken);
        jwtUtils.deleteRefreshToken(memberId);

        // 만료시간까지 남은 시간 동안 블랙리스트에 저장
        redisDao.setValues("blacklist:" + accessToken, "logout", Duration.ofMillis(remainingTime));

        return LogoutMapper.toLogoutResponse();
    }

    /**
     * accessToken이 만료된 사용자가 토큰 재발급 요청을 보내면, access, refresh 토큰 모두 재발급한다.
     *
     * @param refreshToken: accessToken이 만료된 사용자의 refreshToken
     * @return RefreshResponse: accessToken, refreshToken
     */
    @Override
    public RefreshResponse refresh(String refreshToken) {
        jwtUtils.validateRefreshToken(refreshToken);
        String memberId = jwtUtils.getUserNameFromToken(refreshToken);

        // redis에 저장된 기존 refreshToken 제거
        jwtUtils.deleteRefreshToken(memberId);

        // Authentication 객체를 직접 생성
        Collection<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
        Authentication authentication = new UsernamePasswordAuthenticationToken(memberId, null, authorities);

        JwtToken newToken = jwtUtils.generateToken(authentication);
        return RefreshMapper.toRefreshResponse(newToken);
    }

}
