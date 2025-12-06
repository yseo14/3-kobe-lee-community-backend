package com.example.community.Post.repository;

import com.example.community.Post.domain.Post;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
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
}
