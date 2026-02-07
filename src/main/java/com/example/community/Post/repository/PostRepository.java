package com.example.community.Post.repository;

import com.example.community.Post.domain.Post;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostRepository extends JpaRepository<Post, Long>, PostRepositoryCustom {

    /**
     * PostImage와 Image를 함께 조회
     * @param postId 게시글 ID
     * @return Post 엔티티 (PostImage와 Image 포함)
     */
    @Query("SELECT p FROM Post p " +
           "LEFT JOIN FETCH p.postImageList pi " +
           "LEFT JOIN FETCH pi.image " +
           "WHERE p.id = :postId")
    Optional<Post> findByIdWithImages(@Param("postId") Long postId);

    /**
     * 비관적 락(X-Lock)을 걸고 게시글 조회
     * - 데드락 방지: S-Lock → X-Lock 승격 대신, 처음부터 X-Lock 획득
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Post p WHERE p.id = :id")
    Optional<Post> findByIdWithLock(@Param("id") Long id);
}
