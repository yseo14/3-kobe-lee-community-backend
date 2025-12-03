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
import com.example.community.member.domain.Member;
import com.example.community.member.exception.MemberNotFoundException;
import com.example.community.member.repository.MemberRepository;
import com.example.community.postImage.domain.PostImage;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.List;
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
        Post post = postRepository.findById(postId).orElseThrow(PostNotFoundException::new);
        if (!post.getWriter().getId().equals(member.getId())) {
            throw new GeneralException(ErrorStatus.NO_PERMISSION);
        }

        if (updatePostRequest.title() != null) {
            post.updateTitle(updatePostRequest.title());
        }

        if (updatePostRequest.content() != null) {
            post.updateContent(updatePostRequest.content());
        }

        return post;
    }

    @Override
    public List<PostPreview> getPostList(String sort, int limit, Object cursorValue, Long cursorId) {

        return postRepository.findPostsWithCursor(sort, limit, cursorValue, cursorId);
    }

    @Override
    public GetPostResponse getPost(HttpServletRequest httpServletRequest, Long postId) {
        String accessToken = jwtUtils.resolveToken(httpServletRequest);
        Long memberId = Long.parseLong(jwtUtils.getUserNameFromToken(accessToken));

        return postRepository.findPostDetail(postId, memberId);
    }
}
