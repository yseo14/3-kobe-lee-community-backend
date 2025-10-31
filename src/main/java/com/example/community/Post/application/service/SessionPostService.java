package com.example.community.Post.application.service;

import com.example.community.Post.api.dto.CreatePostRequest;
import com.example.community.Post.domain.Post;
import com.example.community.Post.repository.PostRepository;
import com.example.community.global.exception.GeneralException;
import com.example.community.global.response.code.status.ErrorStatus;
import com.example.community.image.domain.Image;
import com.example.community.image.repository.ImageRepository;
import com.example.community.member.domain.Member;
import com.example.community.member.exception.MemberNotFoundException;
import com.example.community.member.repository.MemberRepository;
import com.example.community.postImage.domain.PostImage;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SessionPostService{


    private final PostRepository postRepository;
    private final MemberRepository memberRepository;
    private final ImageRepository imageRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public Post createPost(HttpServletRequest request, CreatePostRequest createPostRequest) {

        Map<String, Object> session = (Map<String, Object>) request.getAttribute("session");
        if (session == null) {
            throw new GeneralException(ErrorStatus._UNAUTHORIZED);
        }

        // 세션에서 memberId 꺼내기
        Long memberId = ((Number) session.get("memberId")).longValue();

        Member member = memberRepository.findById(memberId)
                .orElseThrow(MemberNotFoundException::new);

        Post post = Post.builder()
                .title(createPostRequest.title())
                .content(createPostRequest.content())
                .writer(member)
                .build();

        // 이미지 처리 로직
        List<Long> imageIds = createPostRequest.imageIds();
        for (int i = 0; i < imageIds.size(); i++) {
            Long imageId = imageIds.get(i);

            Image image = imageRepository.findById(imageId)
                    .orElseThrow(() -> new GeneralException(ErrorStatus.IMAGE_NOT_FOUND));

            PostImage postImage = PostImage.of(
                    post,
                    image,
                    i + 1,
                    imageId.equals(createPostRequest.thumbnailImageId())
            );

            post.addPostImage(postImage);
        }

        return postRepository.save(post);
    }


    public LocalDateTime deletePost(HttpServletRequest httpServletRequest, Long postId) {
        return null;
    }

}
