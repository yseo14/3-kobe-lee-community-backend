package com.example.community.Post.application.mapper;

import com.example.community.Post.api.dto.GetPostListResponse;
import com.example.community.Post.api.dto.PostPreview;
import java.util.List;

public class GetPostMapper {
    public static GetPostListResponse toGetPostListResponse(List<PostPreview> postPreviewList, String sort) {
        Object nextCursorValue = null;
        Long nextCursorId = null;

        if (!postPreviewList.isEmpty()) {
            PostPreview last = postPreviewList.get(postPreviewList.size() - 1);
            nextCursorValue = extractCursorValue(last, sort);
            nextCursorId = last.postId();
        }

        return new GetPostListResponse(postPreviewList, nextCursorValue, nextCursorId);
    }

    private static Object extractCursorValue(PostPreview postPreview, String sort) {
        return switch (sort) {
            case "createdAt" -> postPreview.createdAt();
            case "likes" -> postPreview.likeCount();
            case "comments" -> postPreview.commentCount();
            case "views" -> postPreview.viewCount();
            default -> throw new IllegalArgumentException("지원하지 않는 정렬 기준입니다: " + sort);
        };
    }
}
