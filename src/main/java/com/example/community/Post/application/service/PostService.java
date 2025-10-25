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
}
