package com.example.community.comment.api.dto;

import java.time.LocalDateTime;

public record CommentResponse(
        Long commentId,
        String nickname,
        String content,
        String profileImage,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        boolean isUpdated,
        boolean viewerCanEdit,
        boolean viewerCanDelete
) {
}
