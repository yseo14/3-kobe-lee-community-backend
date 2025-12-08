package com.example.community.member.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SignUpRequest(

        String profileImageObjectKey,

        @NotBlank(message = "이메일을 입력해주세요.")
        @Pattern(
                regexp = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$",
                message = "올바른 이메일 주소 형식을 입력해주세요."
        )
        String email,

        @NotEmpty(message = "닉네임을 입력해주세요.")
        @Size(max = 10, message = "닉네임은 최대 10자 까지 작성 가능합니다.")
        @Pattern(regexp = "^(|\\S+)$", message = "띄어쓰기를 없애주세요.")
        String nickname,

        @NotBlank(message = "비밀번호를 입력해주세요.")
        @Pattern(
                regexp = "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,20}$",
                message = "비밀번호는 8자 이상, 20자 이하이며, 대문자, 소문자, 숫자, 특수문자를 각각 최소 1개 포함해야 합니다."
        )
        String password,

        @NotBlank(message = "비밀번호 확인을 입력해주세요.")
        String confirmPassword
) {
}
