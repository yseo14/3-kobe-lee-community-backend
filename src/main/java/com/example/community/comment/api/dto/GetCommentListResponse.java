package com.example.community.comment.api.dto;

import java.time.LocalDateTime;
import java.util.List;

public record GetCommentListResponse(
        List<CommentResponse> commentList,
        LocalDateTime nextCursorCreatedAt,
        Long nextCursorId
) {
}
