package com.example.community.member.application.mapper;

import com.example.community.image.domain.Image;
import com.example.community.member.api.dto.SignUpRequest;
import com.example.community.member.api.dto.SignUpResponse;
import com.example.community.member.domain.Member;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SignUpMapper {
    public static Member toMember(SignUpRequest request, String imageKey, String encodedPassword) {
        return Member.builder()
                .email(request.email())
                .nickname(request.nickname())
                .password(encodedPassword)
                .profileImageKey(imageKey)
                .build();
    }

    public static SignUpResponse toSignUpResponse(Member member) {
        return new SignUpResponse(member.getId());
    }
}
