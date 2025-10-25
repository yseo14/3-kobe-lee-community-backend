package com.example.community.Post.repository;

import com.example.community.Post.api.dto.GetPostResponse;
import com.example.community.Post.api.dto.PostPreview;
import com.example.community.Post.domain.QPost;
import com.example.community.Post.exception.PostNotFoundException;
import com.example.community.image.domain.Image;
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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PostRepositoryCustomImpl implements PostRepositoryCustom {

    private final JPAQueryFactory queryFactory;
    private final QPost post = QPost.post;
    private final QMember member = QMember.member;
    private final QPostImage postImage = QPostImage.postImage;
    private final QImage image = QImage.image;

    @Override
    public List<PostPreview> findPostsWithCursor(String sort, int limit, Object cursor) {
        BooleanExpression cursorCondition = buildCursorCondition(sort, cursor);
        OrderSpecifier<?> orderSpecifier = buildOrderSpecifier(sort);

        return queryFactory
                .select(Projections.constructor(
                        PostPreview.class,
                        post.id,
                        post.title,
                        post.likeCount,
                        post.commentCount,
                        post.viewCount,
                        post.writer.id,
                        image.objectKey,
                        post.writer.nickname,
                        post.createdAt
                ))
                .from(post)
                .join(post.writer, QMember.member)
                .leftJoin(QMember.member.profileImage, image)
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
                        image.objectKey,
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
                                .as("viewerCanDelete")
                ))
                .from(post)
                .join(post.writer, member)
                .leftJoin(member.profileImage, image)
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
                temp.viewerCanDelete()
        );

    }

    private BooleanExpression buildCursorCondition(String sort, Object cursor) {
        if (cursor == null) {
            return null;
        }

        switch (sort) {
            case "createdAt":
                return post.createdAt.lt((LocalDateTime) cursor);
            case "likes":
                return post.likeCount.lt((Long) cursor);
            case "comments":
                return post.commentCount.lt((Long) cursor);
            case "views":
                return post.viewCount.lt((Long) cursor);
            default:
                return null;
        }
    }

    private OrderSpecifier<?> buildOrderSpecifier(String sort) {
        switch (sort) {
            case "createdAt":
                return new OrderSpecifier<>(Order.DESC, post.createdAt);
            case "likes":
                return new OrderSpecifier<>(Order.DESC, post.likeCount);
            case "comments":
                return new OrderSpecifier<>(Order.DESC, post.commentCount);
            case "views":
                return new OrderSpecifier<>(Order.DESC, post.viewCount);
            default:
                return new OrderSpecifier<>(Order.DESC, post.createdAt);
        }
    }
}
