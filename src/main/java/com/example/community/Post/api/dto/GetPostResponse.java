package com.example.community.Post.api.dto;

import java.time.LocalDateTime;
import java.util.List;

public record GetPostResponse(
        Long postId,
        Long memberId,
        String nickname,
        String profileImageKey,
        LocalDateTime createdAt,
        List<String> imageKeyList,
        String title,
        String content,
        Long likeCount,
        Long commentCount,
        Long viewCount,
        boolean viewerCanEdit,
        boolean viewerCanDelete
) {
}