package com.example.community.comment.application.mapper;

import com.example.community.comment.api.dto.CommentResponse;
import com.example.community.comment.api.dto.GetCommentListResponse;
import java.time.LocalDateTime;
import java.util.List;

public class GetCommentMapper {
    public static GetCommentListResponse toGetCommentListResponse(List<CommentResponse> commentResponseList) {
        LocalDateTime nextCursorCreatedAt = null;
        Long nextCursorId = null;

        if (!commentResponseList.isEmpty()) {
            CommentResponse last = commentResponseList.get(commentResponseList.size() - 1);

            nextCursorCreatedAt = last.createdAt();
            nextCursorId = last.commentId();
        }

        return new GetCommentListResponse(commentResponseList, nextCursorCreatedAt, nextCursorId);
    }
}
