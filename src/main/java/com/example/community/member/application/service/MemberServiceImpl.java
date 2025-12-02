package com.example.community.member.application.service;

import com.example.community.auth.jwt.JwtUtils;
import com.example.community.image.exception.InvalidPathException;
import com.example.community.image.service.S3ImageService;
import com.example.community.member.api.dto.InfoResponse;
import com.example.community.member.api.dto.SignUpRequest;
import com.example.community.member.api.dto.UpdateInfoRequest;
import com.example.community.member.api.dto.UpdatePasswordRequest;
import com.example.community.member.application.mapper.InfoMapper;
import com.example.community.member.application.mapper.SignUpMapper;
import com.example.community.member.domain.Member;
import com.example.community.member.exception.DuplicateEmailException;
import com.example.community.member.exception.DuplicateNicknameException;
import com.example.community.member.exception.MemberNotFoundException;
import com.example.community.member.exception.PasswordMismatchException;
import com.example.community.member.repository.MemberRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MemberServiceImpl implements MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final S3ImageService s3ImageService;

    @Value("${app.image.default-profile-key:public/image/common/default_profile.png}")
    private String defaultProfileImageKey;

    @Override
    @Transactional(readOnly = true)
    public boolean emailDuplicateCheck(String email) {
        return !memberRepository.existsByEmail(email);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean nicknameDuplicateCheck(String nickname) {
        return !memberRepository.existsByNickname(nickname);
    }

    @Override
    @Transactional
    public Member signUp(SignUpRequest request) {
        if (memberRepository.existsByEmail(request.email())) {
            throw new DuplicateEmailException();
        }

        if (!request.password().equals(request.confirmPassword())) {
            throw new PasswordMismatchException();
        }

        if (memberRepository.existsByNickname(request.nickname())) {
            throw new DuplicateNicknameException();
        }

        String finalImageKey = defaultProfileImageKey; // 기본값 설정

        if (request.profileImageObjectKey() != null && !request.profileImageObjectKey().isBlank()) {
            String tempKey = request.profileImageObjectKey();

            // 보안 검증: 진짜 temp 폴더 파일인지 확인
            if (!tempKey.startsWith("temp/")) {
                throw new InvalidPathException("잘못된 이미지 경로입니다.");
            }

            String newKey = tempKey.replace("temp/", "public/image/");

            // S3 이동 실행 (Copy & Delete)
            s3ImageService.moveImage(tempKey, newKey);

            finalImageKey = newKey;
        }

        String encodedPassword = passwordEncoder.encode(request.password());
        Member member = SignUpMapper.toMember(request, finalImageKey, encodedPassword);
        memberRepository.save(member);
        return member;
    }

    @Override
    @Transactional(readOnly = true)
    public Member getMemberInfo(Long memberId) {

        return memberRepository.findById(memberId).orElseThrow(MemberNotFoundException::new);
    }

    @Override
    @Transactional
    public Member updateInfo(HttpServletRequest httpServletRequest, UpdateInfoRequest updateInfoRequest) {
        String accessToken = jwtUtils.resolveToken(httpServletRequest);
        String memberId = jwtUtils.getUserNameFromToken(accessToken);
        Member member = memberRepository.findById(Long.parseLong(memberId)).orElseThrow(MemberNotFoundException::new);
        if (updateInfoRequest.nickname() != null) {
            member.updateNickname(updateInfoRequest.nickname());
        }

        if (updateInfoRequest.profileImageKey() != null) {
            member.updateProfileImage(updateInfoRequest.profileImageKey());
        }

        return member;
    }

    @Override
    @Transactional
    public LocalDateTime updatePassword(HttpServletRequest httpServletRequest,
                                        UpdatePasswordRequest updatePasswordRequest) {
        String accessToken = jwtUtils.resolveToken(httpServletRequest);
        String memberId = jwtUtils.getUserNameFromToken(accessToken);
        Member member = memberRepository.findById(Long.parseLong(memberId)).orElseThrow(MemberNotFoundException::new);

        member.updatePassword(passwordEncoder.encode(updatePasswordRequest.password()));
        return LocalDateTime.now();
    }

    @Override
    @Transactional
    public LocalDateTime deleteMember(HttpServletRequest httpServletRequest) {
        String accessToken = jwtUtils.resolveToken(httpServletRequest);
        String memberId = jwtUtils.getUserNameFromToken(accessToken);
        Member member = memberRepository.findById(Long.parseLong(memberId)).orElseThrow(MemberNotFoundException::new);

        return member.deleteMember(LocalDateTime.now());
    }

    @Override
    public InfoResponse getMyInfo(HttpServletRequest httpServletRequest) {
        String accessToken = jwtUtils.resolveToken(httpServletRequest);
        String memberId = jwtUtils.getUserNameFromToken(accessToken);
        Member member = memberRepository.findById(Long.parseLong(memberId)).orElseThrow(MemberNotFoundException::new);

        return InfoMapper.toInfoResponse(member);
    }

}
