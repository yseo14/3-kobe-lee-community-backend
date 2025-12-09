package com.example.community.global.redis;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Component;

// Redis 데이터 접근을 위한 클래스
@Component
public class RedisDao {
    private final RedisTemplate<String, Object> redisTemplate;
    private final ValueOperations<String, Object> values;
    private final SetOperations<String, Object> setOperations;

    public RedisDao(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.values = redisTemplate.opsForValue(); // String 타입을 쉽게 처리하는 메서드
        this.setOperations = redisTemplate.opsForSet(); // Set 타입을 쉽게 처리하는 메서드
    }

    // 기본 데이터 저장
    public void setValues(String key, String data) {
        values.set(key, data);
    }

    // 만료 시간이 있는 데이터 저장
    // 주로 RefreshToken 저장할 때 주로 사용함
    public void setValues(String key, String data, Duration duration) {
        values.set(key, data, duration);
    }

    // 데이터 조회
    // RefreshToken 검증 시 사용됨
    public Object getValues(String key) {
        return values.get(key);
    }

    // 데이터 삭제
    // 로그아웃 시 RefreshToken을 삭제할 때 사용함
    public void deleteValues(String key) {
        redisTemplate.delete(key);
    }

    // ========== 조회수 관련 메서드 ==========
    /**
     * 조회수 증가
     * @param key Redis 키 (예: "post:view:1")
     * @return 증가된 값
     */
    public Long incrementViewCount(String key) {
        return values.increment(key);
    }

    /**
     * 조회 로그 설정 (SETNX - 중복 방지)
     * @param key Redis 키 (예: "view:log:1:123")
     * @param value 값
     * @param duration TTL
     * @return true: 설정 성공 (중복 아님), false: 이미 존재 (중복)
     */
    public Boolean setIfAbsent(String key, String value, Duration duration) {
        return values.setIfAbsent(key, value, duration);
    }

    /**
     * 조회수 값 조회 후 삭제 (GETDEL)
     * @param key Redis 키
     * @return 조회수 값 (없으면 null)
     */
    public Long getAndDelete(String key) {
        Object value = values.getAndDelete(key);
        if (value == null) {
            return null;
        }
        if (value instanceof String) {
            return Long.parseLong((String) value);
        }
        if (value instanceof Long) {
            return (Long) value;
        }
        if (value instanceof Integer) {
            return ((Integer) value).longValue();
        }
        return null;
    }

    // ========== 좋아요 관련 메서드 ==========
    /**
     * 좋아요 추가 (SADD)
     * @param key Redis 키 (예: "post:like:1")
     * @param memberId 멤버 ID
     * @return 추가된 멤버 수 (1: 추가됨, 0: 이미 존재)
     */
    public Long addLike(String key, Long memberId) {
        return setOperations.add(key, memberId.toString());
    }

    /**
     * 좋아요 제거 (SREM)
     * @param key Redis 키
     * @param memberId 멤버 ID
     * @return 제거된 멤버 수 (1: 제거됨, 0: 존재하지 않음)
     */
    public Long removeLike(String key, Long memberId) {
        return setOperations.remove(key, memberId.toString());
    }

    /**
     * 좋아요 여부 확인 (SISMEMBER)
     * @param key Redis 키
     * @param memberId 멤버 ID
     * @return true: 좋아요 함, false: 좋아요 안 함
     */
    public Boolean isMember(String key, Long memberId) {
        return setOperations.isMember(key, memberId.toString());
    }

    /**
     * 좋아요 멤버 ID 하나 추출 후 제거 (SPOP)
     * @param key Redis 키
     * @return 멤버 ID (없으면 null)
     */
    public Long popLike(String key) {
        Object member = setOperations.pop(key);
        if (member == null) {
            return null;
        }
        if (member instanceof String) {
            return Long.parseLong((String) member);
        }
        return null;
    }

    /**
     * 좋아요 멤버 ID 여러 개 추출 후 제거 (SPOP count)
     * @param key Redis 키
     * @param count 추출할 개수
     * @return 멤버 ID 리스트
     */
    public List<Object> popLikes(String key, long count) {
        return setOperations.pop(key, count);
    }

    /**
     * 좋아요 멤버 ID 전체 조회 (SMEMBERS)
     * @param key Redis 키
     * @return 멤버 ID Set
     */
    public Set<Object> getAllLikes(String key) {
        return setOperations.members(key);
    }

    // ========== 키 스캔 관련 메서드 ==========

    /**
     * 패턴에 맞는 키 스캔 (SCAN)
     * @param pattern 키 패턴 (예: "post:view:*")
     * @return 키 Set
     */
    public Set<String> scanKeys(String pattern) {
        Set<String> keys = new java.util.HashSet<>();
        ScanOptions options = ScanOptions.scanOptions()
                .match(pattern)
                .count(100) // 한 번에 스캔할 개수
                .build();

        try (Cursor<String> cursor = redisTemplate.scan(options)) {
            while (cursor.hasNext()) {
                keys.add(cursor.next());
            }
        }
        return keys;
    }

    // ========== 배치 조회 메서드 (성능 최적화) ==========

    /**
     * 여러 게시글의 조회수를 한 번에 조회 (MGET)
     * @param keys 조회수 키 리스트 (예: ["post:view:1", "post:view:2", ...])
     * @return 키와 조회수 값의 Map (키가 없으면 null)
     */
    public Map<String, Long> batchGetViewCounts(List<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return new HashMap<>();
        }

        // MGET으로 여러 키를 한 번에 조회
        List<Object> redisValues = values.multiGet(keys);
        Map<String, Long> result = new HashMap<>();

        for (int i = 0; i < keys.size(); i++) {
            String key = keys.get(i);
            Object value = (redisValues != null && i < redisValues.size()) ? redisValues.get(i) : null;
            Long viewCount = parseLong(value);
            if (viewCount != null) {
                result.put(key, viewCount);
            }
        }

        return result;
    }

    /**
     * 여러 게시글의 좋아요 개수를 한 번에 조회 (Pipeline 사용)
     * @param keys 좋아요 키 리스트 (예: ["post:like:1", "post:like:2", ...])
     * @return 키와 좋아요 개수의 Map (키가 없으면 0)
     */
    public Map<String, Long> batchGetLikeCounts(List<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return new HashMap<>();
        }

        // Pipeline을 사용하여 여러 SCARD 명령을 한 번에 실행
        List<Object> results = redisTemplate.executePipelined(
                (RedisCallback<Object>) connection -> {
                    for (String key : keys) {
                        connection.sCard(key.getBytes());
                    }
                    return null;
                }
        );

        Map<String, Long> result = new HashMap<>();
        for (int i = 0; i < keys.size(); i++) {
            String key = keys.get(i);
            Object count = (results != null && i < results.size()) ? results.get(i) : null;
            Long likeCount = parseLong(count);
            result.put(key, likeCount != null ? likeCount : 0L);
        }

        return result;
    }

    /**
     * Object를 Long으로 파싱 (배치 조회용)
     */
    private Long parseLong(Object obj) {
        if (obj == null) {
            return null;
        }
        if (obj instanceof Long) {
            return (Long) obj;
        }
        if (obj instanceof Integer) {
            return ((Integer) obj).longValue();
        }
        if (obj instanceof String) {
            try {
                return Long.parseLong((String) obj);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
}