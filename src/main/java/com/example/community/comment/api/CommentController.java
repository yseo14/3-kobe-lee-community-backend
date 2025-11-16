package com.example.community.comment.api;

import com.example.community.comment.api.dto.CommentResponse;
import com.example.community.comment.api.dto.CreateCommentRequest;
import com.example.community.comment.api.dto.CreateCommentResponse;
import com.example.community.comment.api.dto.DeleteCommentResponse;
import com.example.community.comment.api.dto.GetCommentListResponse;
import com.example.community.comment.api.dto.UpdateCommentRequest;
import com.example.community.comment.api.dto.UpdateCommentResponse;
import com.example.community.comment.application.mapper.CreateCommentMapper;
import com.example.community.comment.application.mapper.DeleteCommentMapper;
import com.example.community.comment.application.mapper.GetCommentMapper;
import com.example.community.comment.application.mapper.UpdateCommentMapper;
import com.example.community.comment.application.service.CommentService;
import com.example.community.comment.domain.Comment;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/post")
@RequiredArgsConstructor
public class CommentController {
    private final CommentService commentService;

    @PostMapping("/{postId}/comment")
    public ApiResponse<CreateCommentResponse> createComment(HttpServletRequest httpServletRequest,
                                                            @RequestBody @Valid CreateCommentRequest createCommentRequest,
                                                            @PathVariable Long postId) {
        Comment comment = commentService.createComment(httpServletRequest, createCommentRequest, postId);
        return ApiResponse.onSuccess(SuccessStatus.CREATE_COMMENT,
                CreateCommentMapper.toCreateCommentResponse(comment));
    }

    @DeleteMapping("/{postId}/comment/{commentId}")
    public ApiResponse<DeleteCommentResponse> deleteComment(HttpServletRequest httpServletRequest,
                                                            @PathVariable Long postId,
                                                            @PathVariable Long commentId) {
        LocalDateTime deletedAt = commentService.deleteComment(httpServletRequest, postId, commentId);
        return ApiResponse.onSuccess(SuccessStatus.DELETE_COMMENT,
                DeleteCommentMapper.toDeleteCommentResponse(deletedAt));
    }

    @PatchMapping("/{postId}/comment/{commentId}")
    public ApiResponse<UpdateCommentResponse> updateComment(HttpServletRequest httpServletRequest,
                                                            @RequestBody UpdateCommentRequest updateCommentRequest,
                                                            @PathVariable Long postId,
                                                            @PathVariable Long commentId) {
        Comment comment = commentService.updateComment(httpServletRequest, updateCommentRequest, commentId);
        return ApiResponse.onSuccess(SuccessStatus.UPDATE_COMMENT,
                UpdateCommentMapper.toUpdateCommentResponse(comment));
    }

    @GetMapping("/{postId}/comment")
    public ApiResponse<GetCommentListResponse> getCommentList(HttpServletRequest httpServletRequest,
                                                              @PathVariable Long postId,
                                                              @RequestParam(defaultValue = "createdAt") String sort,
                                                              @RequestParam(defaultValue = "5") int limit,
                                                              @RequestParam(required = false)  LocalDateTime cursorCreatedAt,
                                                              @RequestParam(required = false) Long cursorId
                                                              ){
        List<CommentResponse> commentResponseList = commentService.getCommentList(httpServletRequest, postId, sort,
                limit, cursorCreatedAt, cursorId);

        return ApiResponse.onSuccess(SuccessStatus.GET_COMMENT_LIST,
                GetCommentMapper.toGetCommentListResponse(commentResponseList));
    }
}
