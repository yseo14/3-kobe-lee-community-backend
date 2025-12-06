package com.example.community.Post.api.dto;

import java.time.LocalDateTime;

public record PostPreview(
        Long postId,
        String title,
        Long likeCount,
        Long commentCount,
        Long viewCount,
        Long memberId,
        String profileImageKey,
        String nickname,
        LocalDateTime createdAt
) {
}
