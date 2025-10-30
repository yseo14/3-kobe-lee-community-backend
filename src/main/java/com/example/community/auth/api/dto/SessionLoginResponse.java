package com.example.community.auth.api.dto;

public record SessionLoginResponse(
    Long memberId,
    String nickname
) {
}
