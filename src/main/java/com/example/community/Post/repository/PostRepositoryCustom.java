package com.example.community.Post.repository;

import com.example.community.Post.api.dto.GetPostResponse;
import com.example.community.Post.api.dto.PostPreview;
import java.util.List;
import java.util.Map;

public interface PostRepositoryCustom {
    List<PostPreview> findPostsWithCursor(String sort, int limit, Object cursorValue, Long cursorId);

    GetPostResponse findPostDetail(Long postId, Long viewerId);

    /**
     * 조회수 일괄 업데이트
     * @param viewCountMap postId -> 증가시킬 조회수
     */
    void batchUpdateViewCount(Map<Long, Long> viewCountMap);

    /**
     * 좋아요 개수 감소
     * @param postId 게시글 ID
     * @param count 감소시킬 개수
     */
    void decreaseLikeCount(Long postId, Long count);

    /**
     * 좋아요 개수 증가
     * @param postId 게시글 ID
     * @param count 증가시킬 개수
     */
    void increaseLikeCount(Long postId, Long count);
}