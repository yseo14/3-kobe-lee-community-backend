package com.example.community.Post.repository;

import com.example.community.Post.api.dto.GetPostResponse;
import com.example.community.Post.api.dto.PostPreview;
import com.example.community.Post.domain.QPost;
import com.example.community.Post.exception.PostNotFoundException;
import com.example.community.image.domain.QImage;
import com.example.community.member.domain.QMember;
import com.example.community.postImage.domain.QPostImage;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
public class PostRepositoryCustomImpl implements PostRepositoryCustom {

    private final JPAQueryFactory queryFactory;
    private final QPost post = QPost.post;
    private final QMember member = QMember.member;
    private final QPostImage postImage = QPostImage.postImage;
    private final QImage image = QImage.image;

    @Override
    public List<PostPreview> findPostsWithCursor(String sort, int limit, Object cursorValue, Long cursorId) {
        BooleanExpression cursorCondition = buildCursorCondition(sort, cursorValue, cursorId);
        OrderSpecifier<?>[] orderSpecifier = buildOrderSpecifier(sort);
        return queryFactory
                .select(Projections.constructor(
                        PostPreview.class,
                        post.id,
                        post.title,
                        post.likeCount,
                        post.commentCount,
                        post.viewCount,
                        post.writer.id,
                        member.profileImageKey,
                        post.writer.nickname,
                        post.createdAt
                ))
                .from(post)
                .join(post.writer, member)
                .where(cursorCondition)
                .orderBy(orderSpecifier)
                .limit(limit)
                .fetch();
    }

    @Override
    public GetPostResponse findPostDetail(Long postId, Long viewerId) {
        GetPostResponse temp = queryFactory
                .select(Projections.constructor(
                        GetPostResponse.class,
                        post.id,
                        member.id,
                        member.nickname,
                        member.profileImageKey,
                        post.createdAt,
                        Expressions.nullExpression(List.class),
                        post.title,
                        post.content,
                        post.likeCount,
                        post.commentCount,
                        post.viewCount,
                        post.writer.id.eq(viewerId)
                                .as("viewerCanEdit"),
                        post.writer.id.eq(viewerId)
                                .as("viewerCanDelete"),
                        Expressions.constant(false) // isLiked는 Service 레이어에서 Redis 확인 후 설정
                ))
                .from(post)
                .join(post.writer, member)
                .where(post.id.eq(postId))
                .fetchOne();

        if (temp == null) {
            throw new PostNotFoundException();
        }

        List<String> imageKeyList = queryFactory
                .select(postImage.image.objectKey)
                .from(postImage)
                .join(postImage.image, image)
                .where(postImage.post.id.eq(postId))
                .orderBy(
                        postImage.isThumbnail.desc(),
                        postImage.displayOrder.asc()
                )
                .fetch();

        return new GetPostResponse(
                temp.postId(),
                temp.memberId(),
                temp.nickname(),
                temp.profileImageKey(),
                temp.createdAt(),
                imageKeyList,
                temp.title(),
                temp.content(),
                temp.likeCount(),
                temp.commentCount(),
                temp.viewCount(),
                temp.viewerCanEdit(),
                temp.viewerCanDelete(),
                false // isLiked는 Service 레이어에서 Redis 확인 후 설정
        );

    }

    private BooleanExpression buildCursorCondition(String sort, Object cursorValue, Long cursorId) {
        if (cursorValue == null || cursorId == null) {
            return null; // 첫 페이지 요청
        }

        switch (sort) {
            case "createdAt":
                return post.createdAt.lt((LocalDateTime) cursorValue)
                        .or(post.createdAt.eq((LocalDateTime) cursorValue).and(post.id.lt(cursorId)));

            case "likes":
                return post.likeCount.lt((Long) cursorValue)
                        .or(post.likeCount.eq((Long) cursorValue).and(post.id.lt(cursorId)));

            case "comments":
                return post.commentCount.lt((Long) cursorValue)
                        .or(post.commentCount.eq((Long) cursorValue).and(post.id.lt(cursorId)));

            case "views":
                return post.viewCount.lt((Long) cursorValue)
                        .or(post.viewCount.eq((Long) cursorValue).and(post.id.lt(cursorId)));

            default:
                return null;
        }
    }

    private OrderSpecifier<?>[] buildOrderSpecifier(String sort) {
        switch (sort) {
            case "createdAt":
                return new OrderSpecifier[]{
                        new OrderSpecifier<>(Order.DESC, post.createdAt),
                        new OrderSpecifier<>(Order.DESC, post.id)
                };
            case "likes":
                return new OrderSpecifier[]{
                        new OrderSpecifier<>(Order.DESC, post.likeCount),
                        new OrderSpecifier<>(Order.DESC, post.id)
                };
            case "comments":
                return new OrderSpecifier[]{
                        new OrderSpecifier<>(Order.DESC, post.commentCount),
                        new OrderSpecifier<>(Order.DESC, post.id)
                };
            case "views":
                return new OrderSpecifier[]{
                        new OrderSpecifier<>(Order.DESC, post.viewCount),
                        new OrderSpecifier<>(Order.DESC, post.id)
                };
            default:
                return new OrderSpecifier[]{
                        new OrderSpecifier<>(Order.DESC, post.createdAt),
                        new OrderSpecifier<>(Order.DESC, post.id)
                };
        }
    }

    @Override
    @Transactional
    public void batchUpdateViewCount(Map<Long, Long> viewCountMap) {
        if (viewCountMap == null || viewCountMap.isEmpty()) {
            return;
        }

        // QueryDSL을 사용한 일괄 업데이트
        for (Map.Entry<Long, Long> entry : viewCountMap.entrySet()) {
            Long postId = entry.getKey();
            Long increment = entry.getValue();
            
            if (increment != null && increment > 0) {
                queryFactory
                        .update(post)
                        .set(post.viewCount, post.viewCount.add(increment))
                        .where(post.id.eq(postId))
                        .execute();
            }
        }
    }

    @Override
    @Transactional
    public void decreaseLikeCount(Long postId, Long count) {
        if (count == null || count <= 0) {
            return;
        }

        queryFactory
                .update(post)
                .set(post.likeCount, post.likeCount.subtract(count))
                .where(post.id.eq(postId))
                .execute();
    }

    @Override
    @Transactional
    public void increaseLikeCount(Long postId, Long count) {
        if (count == null || count <= 0) {
            return;
        }

        queryFactory
                .update(post)
                .set(post.likeCount, post.likeCount.add(count))
                .where(post.id.eq(postId))
                .execute();
    }
}
