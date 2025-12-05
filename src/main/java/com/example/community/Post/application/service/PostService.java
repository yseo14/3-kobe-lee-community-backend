package com.example.community.Post.application.service;

import com.example.community.Post.api.dto.CreatePostRequest;
import com.example.community.Post.api.dto.GetPostResponse;
import com.example.community.Post.api.dto.PostPreview;
import com.example.community.Post.api.dto.UpdatePostRequest;
import com.example.community.Post.domain.Post;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.List;

public interface PostService {
    Post createPost(HttpServletRequest httpServletRequest, CreatePostRequest createPostRequest);

    LocalDateTime deletePost(HttpServletRequest httpServletRequest, Long postId);

    Post updatePost(HttpServletRequest httpServletRequest, UpdatePostRequest updatePostRequest, Long postId);

    List<PostPreview> getPostList(String sort, int limit, Object cursorValue, Long cursorId);

    GetPostResponse getPost(HttpServletRequest httpServletRequest, Long postId);

    /**
     * 조회수 증가
     * @param postId 게시글 ID
     * @param memberId 조회한 멤버 ID (중복 방지용)
     */
    void incrementViewCount(Long postId, Long memberId);

    /**
     * 좋아요 추가
     * @param postId 게시글 ID
     * @param memberId 좋아요한 멤버 ID
     */
    void likePost(Long postId, Long memberId);

    /**
     * 좋아요 취소
     * @param postId 게시글 ID
     * @param memberId 좋아요 취소한 멤버 ID
     */
    void unlikePost(Long postId, Long memberId);
}
