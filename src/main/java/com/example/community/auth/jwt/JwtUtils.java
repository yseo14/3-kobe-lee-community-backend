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
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Date;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
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
    public JwtToken generateToken(Authentication authentication) {
        // 권한 가져오기
        // JWT 토큰의 claims에 포함되어 사용자의 권한 정보를 저장하는데 사용됨
        String authorities = authentication.getAuthorities().stream() // Authentication 객체에서 사용자 권한 목록 가져오기
                .map(GrantedAuthority::getAuthority) // 각 GrantedAuthority 객체에서 권한 문자열만 추출하기
                .collect(Collectors.joining(",")); // 추출된 권한 문자열들을 쉼표로 구분하여 하나의 문자열로 결합하기

        long now = (new Date()).getTime();
        String username = authentication.getName();

        // AccessToken 생성
        Date accessTokenExpire = new Date(now + ACCESS_TOKEN_EXPIRE_TIME);
        String accessToken = generateAccessToken(username, authorities, accessTokenExpire);

        // RefreshToken 생성
        Date refreshTokenExpire = new Date(now + REFRESH_TOKEN_EXPIRE_TIME);
        String refreshToken = generateRefreshToken(username, refreshTokenExpire);

        // Redis에 RefreshToken 넣기
        // "REFRESH_TOKEN_EXPIRE_TIME"만큼 시간이 지나면 삭제됨
        redisDao.setValues(username, refreshToken, Duration.ofMillis(REFRESH_TOKEN_EXPIRE_TIME));

        return JwtToken.builder()
                .grantType(GRANT_TYPE) // "Bearer"
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    private String generateAccessToken(String username, String authorities, Date expireDate) {
        return Jwts.builder()
                .setSubject(username) // 토큰 제목 (사용자 이름)
                .claim("auth", authorities) // 권한 정보 (커스텀 클레임)
                .setExpiration(expireDate) // 토큰 만료 시간
                .signWith(key, SignatureAlgorithm.HS256) // 지정된 키와 알고리즘으로 서명
                .compact(); // 최종 JWT 문자열 생성 (header.payload.signature 구조);
    }

    private String generateRefreshToken(String username, Date expireDate) {
        return Jwts.builder()
                .setSubject(username)
                .setExpiration(expireDate)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    // JWT 토큰을 복호화하여 토큰에 들어있는 정보 꺼내기
    public Authentication getAuthentication(String accessToken) {
        // JWT 토큰 복호화
        Claims claims = parseClaims(accessToken);
        if (claims.get("auth") == null) {
            throw new RuntimeException("권한 정보가 없는 토큰입니다.");
        }

        // 클레임에서 권한 정보 가져오기
        Collection<? extends GrantedAuthority> authorities = Arrays.stream(claims.get("auth").toString().split(","))
                .map(SimpleGrantedAuthority::new) // SimpleGrantedAuthority 객체들의 컬렉션으로 변환
                .toList();

        // UserDetails 객체를 만들어서 Authentication return
        // UserDetails: interface, User: UserDetails를 구현한 클래스
        UserDetails principal = new User(claims.getSubject(), "", authorities); // 파라미터: 사용자 식별자, credentials, 권한 목록
        return new UsernamePasswordAuthenticationToken(principal, "", authorities);
    }

    // JWT 토큰 복호화
    private Claims parseClaims(String accessToken) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(accessToken) // JWT 토큰 검증과 파싱을 모두 수행함
                    .getBody();
        } catch (MalformedJwtException e) {
            throw new InvalidTokenException(ErrorStatus.INVALID_TOKEN_FORMAT);
        } catch (ExpiredJwtException e) {
            throw new InvalidTokenException(ErrorStatus.EXPIRED_TOKEN);
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
        String username = getUserNameFromToken(token);
        String redisToken = (String) redisDao.getValues(username);

        if (redisToken == null || !redisToken.equals(token)) {
            throw new InvalidTokenException(ErrorStatus.INVALID_REFRESH_TOKEN);
        }
    }

    // 토큰에서 username 추출
    public String getUserNameFromToken(String token) {
        try {
            // 토큰 파싱해서 클레임 얻기
            Claims claims = parseClaims(token);
            // 사용자 이름(subject) 반환
            return claims.getSubject();
        } catch (ExpiredJwtException e) {
            // 토큰이 만료되어도 클레임 내용을 가져올 수 있음
            return e.getClaims().getSubject();
        }
    }

    // RefreshToken 삭제
    public void deleteRefreshToken(String username) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be null or empty");
        }

        // 로그아웃 시 Redis에서 RefreshToken 삭제
        redisDao.deleteValues(username);
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
        if (token == null || token.trim().isEmpty()) {
            return 0;
        }
        
        try {
            Claims claims = parseClaims(token);
            Date expiration = claims.getExpiration();
            long now = System.currentTimeMillis();
            long remaining = expiration.getTime() - now;
            // 만료된 토큰의 경우 0 반환 (음수 방지)
            return Math.max(remaining, 0);
        } catch (ExpiredJwtException e) {
            // 만료된 토큰의 경우 클레임에서 만료 시간 추출
            Date expiration = e.getClaims().getExpiration();
            long now = System.currentTimeMillis();
            long remaining = expiration.getTime() - now;
            return Math.max(remaining, 0);
        } catch (Exception e) {
            // 파싱 실패 시 0 반환
            log.warn("Failed to get remaining expiration for token", e);
            return 0;
        }
    }
}
