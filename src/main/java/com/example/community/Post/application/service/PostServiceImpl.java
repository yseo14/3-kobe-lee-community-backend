package com.example.community.Post.application.service;

import com.example.community.Post.api.dto.CreatePostRequest;
import com.example.community.Post.api.dto.GetPostResponse;
import com.example.community.Post.api.dto.UpdatePostRequest;
import com.example.community.Post.domain.Post;
import com.example.community.Post.exception.PostNotFoundException;
import com.example.community.Post.repository.PostRepository;
import com.example.community.auth.jwt.JwtUtils;
import com.example.community.global.exception.GeneralException;
import com.example.community.global.response.code.status.ErrorStatus;
import com.example.community.Post.api.dto.PostPreview;
import com.example.community.image.domain.Image;
import com.example.community.image.exception.InvalidPathException;
import com.example.community.image.repository.ImageRepository;
import com.example.community.image.service.S3ImageService;
import com.example.community.global.redis.RedisDao;
import com.example.community.member.domain.Member;
import com.example.community.member.exception.MemberNotFoundException;
import com.example.community.member.repository.MemberRepository;
import com.example.community.memberPostLike.domain.MemberPostLikeId;
import com.example.community.memberPostLike.repository.MemberPostLikeRepository;
import com.example.community.postImage.domain.PostImage;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {

    private final JwtUtils jwtUtils;
    private final PostRepository postRepository;
    private final MemberRepository memberRepository;
    private final ImageRepository imageRepository;
    private final S3ImageService s3ImageService;
    private final RedisDao redisDao;
    private final MemberPostLikeRepository memberPostLikeRepository;
    private final EntityManager entityManager;

    @Override
    @Transactional
    public Post createPost(HttpServletRequest httpServletRequest, CreatePostRequest createPostRequest) {
        String accessToken = jwtUtils.resolveToken(httpServletRequest);
        Long memberId = Long.parseLong(jwtUtils.getUserNameFromToken(accessToken));
        Member member = memberRepository.findById(memberId).orElseThrow(MemberNotFoundException::new);

        Post post = Post.builder()
                .title(createPostRequest.title())
                .content(createPostRequest.content())
                .writer(member)
                .build();

        // Post를 먼저 저장하여 id를 생성 (PostImage.of()에서 post.getId()를 사용하므로)
        post = postRepository.save(post);

        List<String> objectKeys = createPostRequest.objectKeys();
        
        // thumbnailObjectKey도 이동된 키로 변환 (비교를 위해)
        String finalThumbnailObjectKey = createPostRequest.thumbnailObjectKey();
        if (finalThumbnailObjectKey != null && finalThumbnailObjectKey.startsWith("temp/")) {
            finalThumbnailObjectKey = finalThumbnailObjectKey.replace("temp/", "public/image/");
        }

        if (objectKeys != null && !objectKeys.isEmpty()) {
            for (int i = 0; i < objectKeys.size(); i++) {
                String objectKey = objectKeys.get(i);
                String finalObjectKey = objectKey;

                // temp 폴더의 이미지인 경우 public 폴더로 이동
                if (objectKey != null && objectKey.startsWith("temp/")) {
                    // 보안 검증: 진짜 temp 폴더 파일인지 확인
                    if (!objectKey.startsWith("temp/")) {
                        throw new InvalidPathException("잘못된 이미지 경로입니다.");
                    }

                    String newKey = objectKey.replace("temp/", "public/image/");

                    // S3 이동 실행 (Copy & Delete)
                    s3ImageService.moveImage(objectKey, newKey);

                    finalObjectKey = newKey;
                }

                // objectKey로 새로운 Image 엔티티 생성 및 저장
                Image image = Image.builder()
                        .objectKey(finalObjectKey)
                        .isUsed(true)
                        .build();
                image = imageRepository.save(image);

                PostImage postImage = PostImage.of(
                        post,
                        image,
                        i + 1,
                        finalObjectKey.equals(finalThumbnailObjectKey)
                );

                post.addPostImage(postImage);
            }
        }

        return postRepository.save(post);
    }

    @Override
    @Transactional
    public LocalDateTime deletePost(HttpServletRequest httpServletRequest, Long postId) {
        String accessToken = jwtUtils.resolveToken(httpServletRequest);
        Long memberId = Long.parseLong(jwtUtils.getUserNameFromToken(accessToken));
        Member member = memberRepository.findById(memberId).orElseThrow(MemberNotFoundException::new);
        Post post = postRepository.findById(postId).orElseThrow(PostNotFoundException::new);

        if (!post.getWriter().getId().equals(member.getId())) {
            throw new GeneralException(ErrorStatus.NO_PERMISSION);
        }

        postRepository.delete(post);
        return LocalDateTime.now();
    }

    @Override
    @Transactional
    public Post updatePost(HttpServletRequest httpServletRequest, UpdatePostRequest updatePostRequest, Long postId) {
        String accessToken = jwtUtils.resolveToken(httpServletRequest);
        Long memberId = Long.parseLong(jwtUtils.getUserNameFromToken(accessToken));
        Member member = memberRepository.findById(memberId).orElseThrow(MemberNotFoundException::new);
        // JOIN FETCH로 PostImage와 Image를 함께 조회 (N+1 문제 해결)
        Post post = postRepository.findByIdWithImages(postId).orElseThrow(PostNotFoundException::new);
        if (!post.getWriter().getId().equals(member.getId())) {
            throw new GeneralException(ErrorStatus.NO_PERMISSION);
        }

        if (updatePostRequest.title() != null) {
            post.updateTitle(updatePostRequest.title());
        }

        if (updatePostRequest.content() != null) {
            post.updateContent(updatePostRequest.content());
        }

        // 이미지 수정 처리
        if (updatePostRequest.objectKeys() != null) {
            List<String> finalObjectKeys = updatePostRequest.objectKeys();
            String finalThumbnailObjectKey = updatePostRequest.thumbnailObjectKey();
            
            // thumbnailObjectKey도 이동된 키로 변환 (비교를 위해)
            if (finalThumbnailObjectKey != null && finalThumbnailObjectKey.startsWith("temp/")) {
                finalThumbnailObjectKey = finalThumbnailObjectKey.replace("temp/", "public/image/");
            }

            // 기존 이미지 정보 저장 (순서 재정렬 전에)
            List<PostImage> existingPostImages = new ArrayList<>(post.getPostImageList());
            // objectKey -> PostImage 매핑 생성 (기존 이미지 찾기용)
            Map<String, PostImage> existingImageMap = existingPostImages.stream()
                    .collect(Collectors.toMap(
                            pi -> pi.getImage().getObjectKey(),
                            pi -> pi,
                            (existing, replacement) -> existing
                    ));

            // 삭제할 기존 이미지 찾기 (최종 리스트에 없는 기존 이미지)
            List<Image> imagesToDelete = new ArrayList<>();
            for (PostImage postImage : existingPostImages) {
                String existingObjectKey = postImage.getImage().getObjectKey();
                
                // 최종 리스트에 없는 기존 이미지는 삭제 대상
                if (!finalObjectKeys.contains(existingObjectKey)) {
                    // S3에서 이미지 삭제
                    s3ImageService.deleteImage(existingObjectKey);
                    // Image 엔티티는 나중에 삭제 (PostImage 삭제 후)
                    imagesToDelete.add(postImage.getImage());
                }
            }

            // PostImage 리스트 초기화 (CascadeType.ALL, orphanRemoval = true로 인해 PostImage 자동 삭제)
            post.getPostImageList().clear();
            
            // Hibernate가 PostImage 삭제를 즉시 DB에 반영하도록 flush
            entityManager.flush();
            
            // PostImage 삭제 후 Image 엔티티 삭제 (외래키 제약조건 해결)
            for (Image imageToDelete : imagesToDelete) {
                imageRepository.delete(imageToDelete);
            }

            // 최종 이미지 리스트 순회: 기존 이미지 유지 + 새 이미지 추가
            for (int i = 0; i < finalObjectKeys.size(); i++) {
                String objectKey = finalObjectKeys.get(i);
                String finalObjectKey = objectKey;
                Image image;
                PostImage postImage;

                // 기존 이미지인지 새 이미지인지 확인
                if (existingImageMap.containsKey(objectKey)) {
                    // 기존 이미지: 유지 (Image 엔티티 재사용)
                    PostImage existingPostImage = existingImageMap.get(objectKey);
                    image = existingPostImage.getImage();
                    finalObjectKey = objectKey; // 이미 public/image/ 경로
                } else {
                    // 새 이미지: temp 폴더에서 public 폴더로 이동
                    if (objectKey == null || !objectKey.startsWith("temp/")) {
                        throw new InvalidPathException("새 이미지는 temp/ 폴더에 있어야 합니다: " + objectKey);
                    }

                    String newKey = objectKey.replace("temp/", "public/image/");
                    
                    try {
                        // S3 이동 실행 (Copy & Delete)
                        s3ImageService.moveImage(objectKey, newKey);
                    } catch (Exception e) {
                        throw new RuntimeException("이미지 이동 실패: " + objectKey + " -> " + newKey + ", 오류: " + e.getMessage(), e);
                    }

                    finalObjectKey = newKey;

                    // 새로운 Image 엔티티 생성 및 저장
                    image = Image.builder()
                            .objectKey(finalObjectKey)
                            .isUsed(true)
                            .build();
                    image = imageRepository.save(image);
                }

                // PostImage 생성 (순서 재정렬)
                postImage = PostImage.of(
                        post,
                        image,
                        i + 1,
                        finalObjectKey.equals(finalThumbnailObjectKey)
                );

                post.addPostImage(postImage);
            }
        }

        return postRepository.save(post);
    }

    @Override
    public List<PostPreview> getPostList(String sort, int limit, Object cursorValue, Long cursorId) {

        return postRepository.findPostsWithCursor(sort, limit, cursorValue, cursorId);
    }

    @Override
    public GetPostResponse getPost(HttpServletRequest httpServletRequest, Long postId) {
        String accessToken = jwtUtils.resolveToken(httpServletRequest);
        Long memberId = Long.parseLong(jwtUtils.getUserNameFromToken(accessToken));

        // DB에서 기본 정보 조회
        GetPostResponse dbResponse = postRepository.findPostDetail(postId, memberId);

        // Redis에서 조회수 합산
        String viewCountKey = "post:view:" + postId;
        Object redisViewCountObj = redisDao.getValues(viewCountKey);
        Long redisViewCount = parseLong(redisViewCountObj);
        Long totalViewCount = dbResponse.viewCount() + (redisViewCount != null ? redisViewCount : 0L);

        // Redis에서 좋아요 개수 합산
        String likeKey = "post:like:" + postId;
        Set<Object> redisLikes = redisDao.getAllLikes(likeKey);
        Long redisLikeCount = (redisLikes != null) ? (long) redisLikes.size() : 0L;
        Long totalLikeCount = dbResponse.likeCount() + redisLikeCount;

        // 좋아요 여부 확인: Redis -> DB 순으로 확인
        boolean isLiked = false;
        if (redisDao.isMember(likeKey, memberId)) {
            isLiked = true;
        } else {
            // Redis에 없으면 DB에서 확인
            MemberPostLikeId id = new MemberPostLikeId(memberId, postId);
            isLiked = memberPostLikeRepository.existsById(id);
        }

        // Redis 값이 합산된 새로운 Response 생성
        return new GetPostResponse(
                dbResponse.postId(),
                dbResponse.memberId(),
                dbResponse.nickname(),
                dbResponse.profileImageKey(),
                dbResponse.createdAt(),
                dbResponse.imageKeyList(),
                dbResponse.title(),
                dbResponse.content(),
                totalLikeCount,
                dbResponse.commentCount(),
                totalViewCount,
                dbResponse.viewerCanEdit(),
                dbResponse.viewerCanDelete(),
                isLiked
        );
    }

    /**
     * Object를 Long으로 파싱
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

    @Override
    public void incrementViewCount(Long postId, Long memberId) {
        // 게시글 존재 여부 확인
        if (!postRepository.existsById(postId)) {
            throw new PostNotFoundException();
        }

        // 중복 조회 방지: 조회 로그 키 (24시간 TTL)
        String viewLogKey = "view:log:" + postId + ":" + memberId;
        Boolean isNewView = redisDao.setIfAbsent(viewLogKey, "1", Duration.ofHours(24));

        // 중복이 아닌 경우에만 조회수 증가
        if (Boolean.TRUE.equals(isNewView)) {
            String viewCountKey = "post:view:" + postId;
            redisDao.incrementViewCount(viewCountKey);
        }
    }

    @Override
    public void likePost(Long postId, Long memberId) {
        // 게시글 존재 여부 확인
        if (!postRepository.existsById(postId)) {
            throw new PostNotFoundException();
        }

        // Redis에 좋아요 추가 (Write-Back: Redis에만 저장, DB는 Scheduler가 동기화)
        String likeKey = "post:like:" + postId;
        redisDao.addLike(likeKey, memberId);
        // 반환값: 1이면 추가됨, 0이면 이미 존재 (무시)
    }

    @Override
    @Transactional
    public void unlikePost(Long postId, Long memberId) {
        // 게시글 존재 여부 확인
        postRepository.findById(postId).orElseThrow(PostNotFoundException::new);

        // Write-Through: Redis와 DB 모두 즉시 삭제
        String likeKey = "post:like:" + postId;
        
        // 1. Redis에서 제거 시도 (있으면 제거, 없으면 그냥 넘어감)
        redisDao.removeLike(likeKey, memberId);

        // 2. DB에서 확인해서 삭제 (Redis에 없어도 DB에 있을 수 있음)
        //    - Scheduler가 이미 Redis → DB 동기화 후 Redis에서 제거했을 수 있음
        MemberPostLikeId id = new MemberPostLikeId(memberId, postId);
        boolean existsInDb = memberPostLikeRepository.existsById(id);
        
        if (existsInDb) {
            memberPostLikeRepository.deleteById(id);
            // 게시글 좋아요 개수 감소
            postRepository.decreaseLikeCount(postId, 1L);
        }
    }
}
