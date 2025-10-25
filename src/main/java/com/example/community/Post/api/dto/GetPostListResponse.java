package com.example.community.Post.api.dto;

import java.util.List;

public record GetPostListResponse(
        List<PostPreview> postList,
        Object nextCursorValue,
        Long nextCursorId
) {
}
