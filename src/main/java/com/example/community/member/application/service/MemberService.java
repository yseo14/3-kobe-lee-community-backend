package com.example.community.member.application.service;

import com.example.community.member.api.dto.InfoResponse;
import com.example.community.member.api.dto.SignUpRequest;
import com.example.community.member.api.dto.UpdateInfoRequest;
import com.example.community.member.api.dto.UpdatePasswordRequest;
import com.example.community.member.domain.Member;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.LocalDateTime;

public interface MemberService {
    boolean emailDuplicateCheck(String email);

    boolean nicknameDuplicateCheck(String nickname);

    Member signUp(SignUpRequest signUpRequest);

    Member getMemberInfo(Long memberId);

    Member updateInfo(HttpServletRequest httpServletRequest, UpdateInfoRequest updateInfoRequest);

    LocalDateTime updatePassword(HttpServletRequest httpServletRequest, UpdatePasswordRequest updatePasswordRequest);

    LocalDateTime deleteMember(HttpServletRequest httpServletRequest);

    InfoResponse getMyInfo(HttpServletRequest httpServletRequest);
}
