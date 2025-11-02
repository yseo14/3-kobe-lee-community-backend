package com.example.community.comment.application.service;

import com.example.community.Post.domain.Post;
import com.example.community.Post.exception.PostNotFoundException;
import com.example.community.Post.repository.PostRepository;
import com.example.community.auth.jwt.JwtUtils;
import com.example.community.comment.api.dto.CommentResponse;
import com.example.community.comment.api.dto.CreateCommentRequest;
import com.example.community.comment.api.dto.UpdateCommentRequest;
import com.example.community.comment.application.mapper.CreateCommentMapper;
import com.example.community.comment.domain.Comment;
import com.example.community.comment.exception.CommentNotFoundException;
import com.example.community.comment.repository.CommentRepository;
import com.example.community.global.exception.GeneralException;
import com.example.community.global.response.code.status.ErrorStatus;
import com.example.community.member.domain.Member;
import com.example.community.member.exception.MemberNotFoundException;
import com.example.community.member.repository.MemberRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {
    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final MemberRepository memberRepository;
    private final JwtUtils jwtUtils;


    @Override
    @Transactional
    public Comment createComment(HttpServletRequest httpServletRequest,
                                 CreateCommentRequest createCommentRequest,
                                 Long postId) {
        String accessToken = jwtUtils.resolveToken(httpServletRequest);
        Long memberId = Long.parseLong(jwtUtils.getUserMemberIdFromToken(accessToken));
        Member member = memberRepository.findById(memberId).orElseThrow(MemberNotFoundException::new);
        Post post = postRepository.findById(postId).orElseThrow(PostNotFoundException::new);
        Comment comment = CreateCommentMapper.toComment(createCommentRequest, member, post);
        post.addComment(comment);

        return commentRepository.save(comment);
    }

    @Override
    @Transactional
    public LocalDateTime deleteComment(HttpServletRequest httpServletRequest, Long postId, Long commentId) {
        String accessToken = jwtUtils.resolveToken(httpServletRequest);
        Long memberId = Long.parseLong(jwtUtils.getUserMemberIdFromToken(accessToken));
        Post post = postRepository.findById(postId).orElseThrow(PostNotFoundException::new);
        Comment comment = commentRepository.findById(commentId).orElseThrow(CommentNotFoundException::new);

        if (!comment.getWriter().getId().equals(memberId)) {
            throw new GeneralException(ErrorStatus.NO_PERMISSION);
        }

        post.removeComment(comment);
        commentRepository.delete(comment);

        return LocalDateTime.now();
    }

    @Override
    @Transactional
    public Comment updateComment(HttpServletRequest httpServletRequest, UpdateCommentRequest updateCommentRequest,
                                 Long commentId) {
        String accessToken = jwtUtils.resolveToken(httpServletRequest);
        Long memberId = Long.parseLong(jwtUtils.getUserMemberIdFromToken(accessToken));
        Comment comment = commentRepository.findById(commentId).orElseThrow(CommentNotFoundException::new);

        if (!comment.getWriter().getId().equals(memberId)) {
            throw new GeneralException(ErrorStatus.NO_PERMISSION);
        }
        comment.updateContent(updateCommentRequest.content());

        return comment;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommentResponse> getCommentList(HttpServletRequest httpServletRequest, Long postId, String sort,
                                                int limit, LocalDateTime cursorCreatedAt,
                                                Long cursorId) {
        String accessToken = jwtUtils.resolveToken(httpServletRequest);
        Long memberId = Long.parseLong(jwtUtils.getUserMemberIdFromToken(accessToken));

        return commentRepository.getCommentListWithCursor(memberId, postId, sort, limit, cursorCreatedAt, cursorId);
    }

}
