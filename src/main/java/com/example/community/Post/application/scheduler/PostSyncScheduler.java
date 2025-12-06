package com.example.community.Post.application.scheduler;

import com.example.community.Post.repository.PostRepository;
import com.example.community.global.redis.RedisDao;
import com.example.community.member.domain.Member;
import com.example.community.member.repository.MemberRepository;
import com.example.community.memberPostLike.domain.MemberPostLike;
import com.example.community.memberPostLike.domain.MemberPostLikeId;
import com.example.community.memberPostLike.repository.MemberPostLikeRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Redis Write-Back 전략을 위한 주기적 동기화 Scheduler
 * - 조회수: Redis -> DB 일괄 업데이트
 * - 좋아요: Redis -> DB 배치 인서트
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PostSyncScheduler {

    private final RedisDao redisDao;
    private final PostRepository postRepository;
    private final MemberPostLikeRepository memberPostLikeRepository;
    private final MemberRepository memberRepository;

    private static final String VIEW_COUNT_PATTERN = "post:view:*";
    private static final String LIKE_PATTERN = "post:like:*";
    private static final int BATCH_SIZE = 100; // 배치 처리 크기

    /**
     * 조회수 동기화 (5분마다 실행)
     * Redis의 조회수 값을 DB에 반영
     */
    @Scheduled(fixedRate = 300000) // 5분 = 300,000ms
    @Transactional
    public void syncViewCounts() {
        log.info("조회수 동기화 시작");
        long startTime = System.currentTimeMillis();

        try {
            // Redis에서 조회수 키 스캔
            Set<String> viewKeys = redisDao.scanKeys(VIEW_COUNT_PATTERN);
            
            if (viewKeys.isEmpty()) {
                log.info("동기화할 조회수 데이터가 없습니다.");
                return;
            }

            Map<Long, Long> viewCountMap = new HashMap<>();

            // 각 키에서 조회수 값 추출 후 삭제
            for (String key : viewKeys) {
                try {
                    // 키에서 postId 추출: "post:view:123" -> 123
                    Long postId = extractPostId(key, "post:view:");
                    if (postId == null) {
                        continue;
                    }

                    // 조회수 값 조회 후 삭제
                    Long viewCount = redisDao.getAndDelete(key);
                    if (viewCount != null && viewCount > 0) {
                        viewCountMap.put(postId, viewCount);
                    }
                } catch (Exception e) {
                    log.error("조회수 동기화 중 오류 발생 - key: {}", key, e);
                }
            }

            // DB에 일괄 업데이트
            if (!viewCountMap.isEmpty()) {
                postRepository.batchUpdateViewCount(viewCountMap);
                log.info("조회수 동기화 완료: {}개 게시글", viewCountMap.size());
            }

        } catch (Exception e) {
            log.error("조회수 동기화 중 전체 오류 발생", e);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            log.info("조회수 동기화 종료 (소요 시간: {}ms)", duration);
        }
    }

    /**
     * 좋아요 동기화 (1분마다 실행)
     * Redis의 좋아요 Set을 DB에 배치 인서트
     */
    @Scheduled(fixedRate = 60000) // 1분 = 60,000ms
    @Transactional
    public void syncLikes() {
        log.info("좋아요 동기화 시작");
        long startTime = System.currentTimeMillis();

        try {
            // Redis에서 좋아요 키 스캔
            Set<String> likeKeys = redisDao.scanKeys(LIKE_PATTERN);
            
            if (likeKeys.isEmpty()) {
                log.info("동기화할 좋아요 데이터가 없습니다.");
                return;
            }

            int totalSynced = 0;

            // 각 키에서 좋아요 멤버 ID 추출 후 삭제 (SPOP)
            for (String key : likeKeys) {
                try {
                    // 키에서 postId 추출: "post:like:123" -> 123
                    Long postId = extractPostId(key, "post:like:");
                    if (postId == null) {
                        continue;
                    }

                    // 게시글 존재 여부 확인
                    if (!postRepository.existsById(postId)) {
                        // 존재하지 않는 게시글의 좋아요는 삭제
                        redisDao.deleteValues(key);
                        continue;
                    }

                    // 좋아요 멤버 ID 배치 추출 (한 번에 최대 BATCH_SIZE개)
                    List<MemberPostLike> likesToInsert = new ArrayList<>();
                    List<Object> memberIds = redisDao.popLikes(key, BATCH_SIZE);

                    while (memberIds != null && !memberIds.isEmpty()) {
                        for (Object memberIdObj : memberIds) {
                            try {
                                Long memberId = parseMemberId(memberIdObj);
                                if (memberId == null) {
                                    continue;
                                }

                                // 이미 DB에 존재하는지 확인
                                MemberPostLikeId id = new MemberPostLikeId(memberId, postId);
                                if (memberPostLikeRepository.existsById(id)) {
                                    continue; // 이미 존재하면 스킵
                                }

                                // Member와 Post 엔티티 조회
                                Member member = memberRepository.findById(memberId).orElse(null);
                                if (member == null) {
                                    continue; // 존재하지 않는 멤버는 스킵
                                }

                                // MemberPostLike 엔티티 생성
                                MemberPostLike memberPostLike = new MemberPostLike(
                                        id,
                                        member,
                                        postRepository.findById(postId).orElse(null),
                                        LocalDateTime.now()
                                );

                                likesToInsert.add(memberPostLike);
                            } catch (Exception e) {
                                log.error("좋아요 처리 중 오류 발생 - postId: {}, memberId: {}", postId, memberIdObj, e);
                            }
                        }

                        // 배치 인서트 수행
                        if (!likesToInsert.isEmpty()) {
                            memberPostLikeRepository.saveAll(likesToInsert);
                            // 게시글 좋아요 개수 증가
                            postRepository.increaseLikeCount(postId, (long) likesToInsert.size());
                            totalSynced += likesToInsert.size();
                            likesToInsert.clear();
                        }

                        // 다음 배치 추출
                        memberIds = redisDao.popLikes(key, BATCH_SIZE);
                        if (memberIds == null) {
                            memberIds = new ArrayList<>();
                        }
                    }

                    // 모든 멤버를 추출한 후 빈 Set이 되면 키 삭제
                    Set<Object> remaining = redisDao.getAllLikes(key);
                    if (remaining == null || remaining.isEmpty()) {
                        redisDao.deleteValues(key);
                    }

                } catch (Exception e) {
                    log.error("좋아요 동기화 중 오류 발생 - key: {}", key, e);
                }
            }

            if (totalSynced > 0) {
                log.info("좋아요 동기화 완료: {}개 좋아요", totalSynced);
            }

        } catch (Exception e) {
            log.error("좋아요 동기화 중 전체 오류 발생", e);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            log.info("좋아요 동기화 종료 (소요 시간: {}ms)", duration);
        }
    }

    /**
     * Redis 키에서 postId 추출
     * @param key Redis 키 (예: "post:view:123")
     * @param prefix 키 접두사 (예: "post:view:")
     * @return postId (추출 실패 시 null)
     */
    private Long extractPostId(String key, String prefix) {
        try {
            if (key.startsWith(prefix)) {
                String postIdStr = key.substring(prefix.length());
                return Long.parseLong(postIdStr);
            }
        } catch (NumberFormatException e) {
            log.warn("postId 추출 실패 - key: {}", key);
        }
        return null;
    }

    /**
     * 멤버 ID 파싱
     * @param memberIdObj 멤버 ID 객체
     * @return 파싱된 멤버 ID (실패 시 null)
     */
    private Long parseMemberId(Object memberIdObj) {
        if (memberIdObj == null) {
            return null;
        }
        if (memberIdObj instanceof String) {
            try {
                return Long.parseLong((String) memberIdObj);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        if (memberIdObj instanceof Long) {
            return (Long) memberIdObj;
        }
        if (memberIdObj instanceof Integer) {
            return ((Integer) memberIdObj).longValue();
        }
        return null;
    }
}

