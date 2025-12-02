package com.example.community.comment.repository;

import com.example.community.Post.domain.QPost;
import com.example.community.comment.api.dto.CommentResponse;
import com.example.community.comment.domain.QComment;
import com.example.community.image.domain.QImage;
import com.example.community.member.domain.QMember;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class CommentRepositoryCustomImpl implements CommentRepositoryCustom {
    private final JPAQueryFactory queryFactory;
    private final QMember writer = QMember.member;
    private final QComment comment = QComment.comment;

    @Override
    public List<CommentResponse> getCommentListWithCursor(Long viewerId, Long postId, String sort, int limit, LocalDateTime cursorCreatedAt,
                                                          Long cursorId) {
        BooleanExpression cursorCondition = buildCursorCondition(sort, cursorCreatedAt, cursorId);
        OrderSpecifier<?>[] orderSpecifier = buildOrderSpecifier(sort);
        return queryFactory
                .select(Projections.constructor(
                        CommentResponse.class,
                        comment.id,
                        writer.nickname,
                        comment.content,
                        writer.profileImageKey,                     // 작성자 프로필 이미지
                        comment.createdAt,
                        comment.updatedAt,                          // 수정된 시각
                        comment.createdAt.ne(comment.updatedAt),    // 생성시간과 수정시간이 다르면 수정된 것
                        writer.id.eq(viewerId)
                                .as("viewerCanEdit"),
                        writer.id.eq(viewerId)
                                .as("viewerCanDelete")
                ))
                .from(comment)
                .join(comment.writer, writer)
                .where(comment.post.id.eq(postId),
                        cursorCondition)
                .orderBy(orderSpecifier)
                .limit(limit)
                .fetch();
    }

    private BooleanExpression buildCursorCondition(String sort, LocalDateTime cursorCreatedAt, Long cursorId) {

        if (cursorCreatedAt == null || cursorId == null) {
            return null; // 첫 페이지 요청
        }

        switch (sort) {
            case "latest":
                // 최신순 (createdAt DESC, id DESC)
                return comment.createdAt.lt(cursorCreatedAt)
                        .or(comment.createdAt.eq(cursorCreatedAt).and(comment.id.lt(cursorId)));

            case "oldest":
                // 등록순 (createdAt ASC, id ASC)
                return comment.createdAt.gt(cursorCreatedAt)
                        .or(comment.createdAt.eq(cursorCreatedAt).and(comment.id.gt(cursorId)));

            default:
                return null;
        }
    }

    private OrderSpecifier<?>[] buildOrderSpecifier(String sort) {
        switch (sort) {
            case "latest":
                return new OrderSpecifier[]{
                        new OrderSpecifier<>(Order.DESC, comment.createdAt),
                        new OrderSpecifier<>(Order.DESC, comment.id)
                };
            case "oldest":
                return new OrderSpecifier[]{
                        new OrderSpecifier<>(Order.ASC, comment.createdAt),
                        new OrderSpecifier<>(Order.ASC, comment.id)
                };
            default:
                return new OrderSpecifier[]{
                        new OrderSpecifier<>(Order.DESC, comment.createdAt),
                        new OrderSpecifier<>(Order.DESC, comment.id)
                };
        }
    }
}
