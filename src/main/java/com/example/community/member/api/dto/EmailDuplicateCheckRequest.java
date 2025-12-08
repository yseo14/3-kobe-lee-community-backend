package com.example.community.member.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record EmailDuplicateCheckRequest(
        @NotBlank(message = "이메일을 입력해주세요.")
        @Pattern(
                regexp = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$",
                message = "올바른 이메일 주소 형식을 입력해주세요."
        )
        String email

) {}