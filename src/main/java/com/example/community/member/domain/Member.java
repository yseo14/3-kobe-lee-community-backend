package com.example.community.member.domain;

import com.example.community.Post.domain.Post;
import com.example.community.global.domain.BaseEntity;
import com.example.community.image.domain.Image;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id")
    private Long id;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false, length = 10)
    private String nickname;

    @Column(nullable = false)
    private String password;

    private LocalDateTime deletedAt;

    @Column(name = "profile_image_key", nullable = false)
    private String profileImageKey;

    @OneToMany(mappedBy = "writer")
    private List<Post> postList = new ArrayList<>();    //  추후 확장시 사용자가 작성한 게시글 목록을 조회해야할 소요가 있으니 양방향 연관관계 설정

    public void updateNickname(String nickname) {
        this.nickname = nickname;
    }

    public void updateProfileImage(String profileImageKey) {
        this.profileImageKey = profileImageKey;
    }

    public void updatePassword(String encodedPassword) {
        this.password = encodedPassword;
    }

    public LocalDateTime deleteMember(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
        return deletedAt;
    }
}
