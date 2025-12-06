package com.example.community.member.api.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateInfoRequest(
        @Size(max = 10, message = "닉네임은 최대 10자 까지 작성 가능합니다.")
        @Pattern(regexp = "^(|\\S+)$", message = "띄어쓰기를 없애주세요.")
        String nickname,
        String profileImageObjectKey
) {
}
