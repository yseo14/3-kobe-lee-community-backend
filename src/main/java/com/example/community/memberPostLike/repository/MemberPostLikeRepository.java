package com.example.community.memberPostLike.repository;

import com.example.community.memberPostLike.domain.MemberPostLike;
import com.example.community.memberPostLike.domain.MemberPostLikeId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberPostLikeRepository extends JpaRepository<MemberPostLike, MemberPostLikeId> {
    boolean existsById(MemberPostLikeId id);
}

