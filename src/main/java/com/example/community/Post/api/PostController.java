package com.example.community.Post.api;

import com.example.community.Post.api.dto.CreatePostRequest;
import com.example.community.Post.api.dto.CreatePostResponse;
import com.example.community.Post.api.dto.GetPostListResponse;
import com.example.community.Post.api.dto.GetPostResponse;
import com.example.community.Post.api.dto.PostPreview;
import com.example.community.Post.api.dto.UpdatePostRequest;
import com.example.community.Post.api.dto.UpdatePostResponse;
import com.example.community.Post.application.mapper.CreatePostMapper;
import com.example.community.Post.application.mapper.GetPostMapper;
import com.example.community.Post.application.mapper.UpdatePostMapper;
import com.example.community.Post.application.service.PostService;
import com.example.community.Post.application.service.SessionPostService;
import com.example.community.Post.domain.Post;
import com.example.community.global.response.ApiResponse;
import com.example.community.global.response.code.status.SuccessStatus;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/post")
@RequiredArgsConstructor
public class PostController {
    private final PostService postService;
    private final SessionPostService sessionPostService;

    @PostMapping
    public ApiResponse<CreatePostResponse> createPost(HttpServletRequest httpServletRequest,
                                                      @RequestBody @Valid CreatePostRequest createPostRequest) {
        Post post = postService.createPost(httpServletRequest, createPostRequest);
        return ApiResponse.onSuccess(SuccessStatus.CREATE_POST, CreatePostMapper.toCreatePostResponse(post));
    }

    @PostMapping("/v2")
    public ApiResponse<CreatePostResponse> sessionCreatePost(HttpServletRequest httpServletRequest,
                                                      @RequestBody @Valid CreatePostRequest createPostRequest) {
        Post post = sessionPostService.createPost(httpServletRequest, createPostRequest);
        return ApiResponse.onSuccess(SuccessStatus.CREATE_POST, CreatePostMapper.toCreatePostResponse(post));
    }

    @DeleteMapping("/{postId}")
    public ApiResponse<LocalDateTime> deletePost(HttpServletRequest httpServletRequest,
                                                 @PathVariable Long postId) {
        return ApiResponse.onSuccess(SuccessStatus.DELETE_POST, postService.deletePost(httpServletRequest, postId));
    }

    @PatchMapping("/{postId}")
    public ApiResponse<UpdatePostResponse> updatePost(HttpServletRequest httpServletRequest,
                                                      @RequestBody @Valid UpdatePostRequest updatePostRequest,
                                                      @PathVariable Long postId) {
        Post post = postService.updatePost(httpServletRequest, updatePostRequest, postId);
        return ApiResponse.onSuccess(SuccessStatus.UPDATE_POST, UpdatePostMapper.toUpdatePostResponse(post));
    }

    @GetMapping
    public ApiResponse<GetPostListResponse> getPostList(
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) String cursorValue,
            @RequestParam(required = false) Long cursorId) {
        Object cursorType = parseCursor(sort, cursorValue, cursorId);

        List<PostPreview> postPreviewList = postService.getPostList(sort, limit, cursorType, cursorId);
        return ApiResponse.onSuccess(SuccessStatus.GET_POST_LIST,
                GetPostMapper.toGetPostListResponse(postPreviewList, sort));
    }

    @GetMapping("/{postId}")
    public ApiResponse<GetPostResponse> getPost(HttpServletRequest httpServletRequest,
                                                @PathVariable Long postId) {
        return ApiResponse.onSuccess(SuccessStatus.GET_POST, postService.getPost(httpServletRequest, postId));
    }


    /**
     * 1. 해당 요청이 첫 요청인지 판단한다. (cursorValue와 cursorId가 null이면 첫 요청) 2. 첫 요청이 아니라면, sort를 기준으로 cursorValue의 타입을 변환한다.
     * (createdAt 즉, 생성일자 기준이면 LocalDateTime으로 변환. 카운터 기준이면 Long으로 변환)
     *
     * @param sort:        정렬 기준
     * @param cursorValue: 다음 데이터를 요청하기 위한 이전 요청의 마지막 데이터의 cursor 값
     * @param cursorId:    이전 요청의 마지막 데이터의 postId 값
     * @return
     */
    private Object parseCursor(String sort, String cursorValue, Long cursorId) {
        if (cursorValue == null || cursorId == null) {
            return null; // 첫 페이지 요청
        }

        return switch (sort) {
            case "createdAt" -> LocalDateTime.parse(cursorValue);   //  최신순이면 커서를 생성일자로 설정
            case "likes", "comments", "views" -> Long.parseLong(cursorValue);   //  카운터순이면 커서를 카운터 값으로 설정
            default -> throw new IllegalArgumentException("지원하지 않는 정렬 기준입니다: " + sort);
        };
    }

}
