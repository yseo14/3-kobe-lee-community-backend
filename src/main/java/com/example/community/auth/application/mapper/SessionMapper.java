package com.example.community.auth.application.mapper;

import com.example.community.auth.api.dto.SessionLoginResponse;
import com.example.community.member.domain.Member;

public class SessionMapper {
    public static SessionLoginResponse toSessionLoginResponse(Member member) {
        return new SessionLoginResponse(member.getId(), member.getNickname());
    }
}
