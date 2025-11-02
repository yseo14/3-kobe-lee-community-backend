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
import com.example.community.image.repository.ImageRepository;
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

    @Override
    @Transactional
    public Post createPost(HttpServletRequest httpServletRequest, CreatePostRequest createPostRequest) {
        String accessToken = jwtUtils.resolveToken(httpServletRequest);
        Long memberId = Long.parseLong(jwtUtils.getUserMemberIdFromToken(accessToken));
        Member member = memberRepository.findById(memberId).orElseThrow(MemberNotFoundException::new);

        Post post = Post.builder()
                .title(createPostRequest.title())
                .content(createPostRequest.content())
                .writer(member)
                .build();

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

    @Override
    @Transactional
    public LocalDateTime deletePost(HttpServletRequest httpServletRequest, Long postId) {
        String accessToken = jwtUtils.resolveToken(httpServletRequest);
        Long memberId = Long.parseLong(jwtUtils.getUserMemberIdFromToken(accessToken));
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
        Long memberId = Long.parseLong(jwtUtils.getUserMemberIdFromToken(accessToken));
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
        Long memberId = Long.parseLong(jwtUtils.getUserMemberIdFromToken(accessToken));

        return postRepository.findPostDetail(postId, memberId);
    }
}
