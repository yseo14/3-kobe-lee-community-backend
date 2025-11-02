package com.example.community.auth.jwt;

import com.example.community.auth.jwt.exception.InvalidTokenException;
import com.example.community.global.redis.RedisDao;
import com.example.community.global.response.code.status.ErrorStatus;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import java.security.Key;
import java.time.Duration;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
public class JwtUtils {
    private final Key key;
    private final RedisDao redisDao; // RefreshToken 저장을 위해 Redis 사용

    private static final String GRANT_TYPE = "Bearer";

    @Value("${jwt.access-token-expiration}")
    private long ACCESS_TOKEN_EXPIRE_TIME;

    @Value("${jwt.refresh-token-expiration}")
    private long REFRESH_TOKEN_EXPIRE_TIME;

    public JwtUtils(@Value("${jwt.secret}") String secretKey,
                    RedisDao redisDao) {
        byte[] keyBytes = Base64.getEncoder().encode(secretKey.getBytes());
        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.redisDao = redisDao;
    }

    // Member 정보를 가지고 AccessToken, RefreshToken을 생성하기
    public JwtToken generateToken(Long memberId, List<String> roles) {
        long now = (new Date()).getTime();

        // AccessToken 생성
        Date accessTokenExpire = new Date(now + ACCESS_TOKEN_EXPIRE_TIME);
        String accessToken = generateAccessToken(String.valueOf(memberId), accessTokenExpire, roles);

        // RefreshToken 생성
        Date refreshTokenExpire = new Date(now + REFRESH_TOKEN_EXPIRE_TIME);
        String refreshToken = generateRefreshToken(String.valueOf(memberId), refreshTokenExpire);

        // Redis에 RefreshToken 넣기
        // "REFRESH_TOKEN_EXPIRE_TIME"만큼 시간이 지나면 삭제됨
        redisDao.setValues(String.valueOf(memberId), refreshToken, Duration.ofMillis(REFRESH_TOKEN_EXPIRE_TIME));

        return JwtToken.builder()
                .grantType(GRANT_TYPE) // "Bearer"
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    public JwtToken generateToken(Long memberId, List<String> roles, long refreshTtlMillis) {
        long now = (new Date()).getTime();

        // AccessToken 생성 (기존과 동일)
        Date accessTokenExpire = new Date(now + ACCESS_TOKEN_EXPIRE_TIME);
        String accessToken = generateAccessToken(String.valueOf(memberId), accessTokenExpire, roles);

        // RefreshToken 만료 시간을 기존 남은 시간으로 지정
        Date refreshTokenExpire = new Date(now + refreshTtlMillis);
        String refreshToken = generateRefreshToken(String.valueOf(memberId), refreshTokenExpire);

        // Redis에 동일 TTL로 저장
        redisDao.setValues(String.valueOf(memberId), refreshToken, Duration.ofMillis(refreshTtlMillis));

        return JwtToken.builder()
                .grantType(GRANT_TYPE)
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    private String generateAccessToken(String memberId, Date expireDate, List<String> roles) {
        return Jwts.builder()
                .setSubject(memberId) // 토큰 제목 (memberId)
                .setExpiration(expireDate) // 토큰 만료 시간
                .setIssuedAt(new Date())
                .claim("roles", roles)
                .signWith(key, SignatureAlgorithm.HS256) // 지정된 키와 알고리즘으로 서명
                .compact(); // 최종 JWT 문자열 생성 (header.payload.signature 구조);
    }

    private String generateRefreshToken(String memberId, Date expireDate) {
        return Jwts.builder()
                .setSubject(memberId)
                .claim("typ", "refresh")
                .setId(UUID.randomUUID().toString())
                .setIssuedAt(new Date())
                .setExpiration(expireDate)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    // JWT 토큰 복호화
    public Claims parseClaims(String accessToken) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(accessToken) // JWT 토큰 검증과 파싱을 모두 수행함
                    .getBody();
        } catch (MalformedJwtException e) {
            throw new InvalidTokenException(ErrorStatus.INVALID_TOKEN_FORMAT);
        } catch (ExpiredJwtException e) {
            return e.getClaims();
        } catch (JwtException e) {
            throw new InvalidTokenException(ErrorStatus.INVALID_TOKEN);
        }
    }

    // 토큰 정보 검증
    public void validateToken(String token) {
        //블랙리스트 토큰 검사
        if (redisDao.getValues("blacklist:" + token) != null) {
            throw new InvalidTokenException(ErrorStatus.BLACKLISTED_TOKEN);
        }

        try {
            Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token);

        } catch (SecurityException | MalformedJwtException e) {
            log.info("Invalid JWT Token", e);
            throw new InvalidTokenException(ErrorStatus.INVALID_TOKEN_FORMAT);

        } catch (ExpiredJwtException e) {
            log.info("Expired JWT Token", e);
            throw new InvalidTokenException(ErrorStatus.EXPIRED_TOKEN);

        } catch (UnsupportedJwtException e) {
            log.info("Unsupported JWT Token", e);
            throw new InvalidTokenException(ErrorStatus.UNSUPPORTED_TOKEN);

        } catch (IllegalArgumentException e) {
            log.info("JWT claims string is empty.", e);
            throw new InvalidTokenException(ErrorStatus.EMPTY_TOKEN);
        }
    }


    // RefreshToken 검증
    public void validateRefreshToken(String token) {
        // 1. 기본 JWT 검증 (유효하지 않으면 예외 발생)
        validateToken(token);

        // 2. Redis 저장값과 비교
        String username = getUserMemberIdFromToken(token);
        String redisToken = (String) redisDao.getValues(username);

        if (redisToken == null || !redisToken.equals(token)) {
            throw new InvalidTokenException(ErrorStatus.INVALID_REFRESH_TOKEN);
        }
    }

    // 토큰에서 memberId 추출
    public String getUserMemberIdFromToken(String token) {
        try {
            // 토큰 파싱해서 클레임 얻기
            Claims claims = parseClaims(token);
            // memberId(subject) 반환
            return claims.getSubject();
        } catch (ExpiredJwtException e) {
            // 토큰이 만료되어도 클레임 내용을 가져올 수 있음
            return e.getClaims().getSubject();
        }
    }

    // RefreshToken 삭제
    public void deleteRefreshToken(String memberId) {
        if (memberId == null || memberId.trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be null or empty");
        }

        // 로그아웃 시 Redis에서 RefreshToken 삭제
        redisDao.deleteValues(memberId);
    }

    // Request Header에서 JWT 토큰 추출
    public String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer")) {
            return bearerToken.substring(7); // "Bearer " 이후만 넘기기
        }
        return null;
    }

    public long getRemainingExpiration(String token) {
        Claims claims = parseClaims(token);
        Date expiration = claims.getExpiration();
        long now = System.currentTimeMillis();
        return expiration.getTime() - now;
    }
}
