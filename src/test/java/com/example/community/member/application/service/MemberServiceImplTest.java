package com.example.community.member.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.example.community.auth.jwt.JwtUtils;
import com.example.community.image.service.S3ImageService;
import com.example.community.member.api.dto.SignUpRequest;
import com.example.community.member.domain.Member;
import com.example.community.member.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class MemberServiceImplTest {
    @InjectMocks
    private MemberServiceImpl memberService;

    @Mock
    private MemberRepository memberRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtUtils jwtUtils;
    @Mock
    private S3ImageService s3ImageService;


    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(memberService, "defaultProfileImageKey", "public/default.png");
    }

    @Test
    @DisplayName("회원가입 성공 테스트")
    void signUp_success() {
        // given
        String email = "test@example.com";
        String password = "password123";
        String nickname = "tester";

        // SignUpRequest(profileImageKey, email, nickname, password, confirmPassword)
        SignUpRequest request = new SignUpRequest(null, email, nickname, password, password);

        given(memberRepository.existsByEmail(email)).willReturn(false);
        given(memberRepository.existsByNickname(nickname)).willReturn(false);
        given(passwordEncoder.encode(password)).willReturn("encodedPassword");

        // save 호출 시 들어온 객체 그대로 반환
        given(memberRepository.save(any(Member.class))).willAnswer(invocation -> invocation.getArgument(0));

        // when
        Member savedMember = memberService.signUp(request);

        // then
        assertThat(savedMember).isNotNull();
        assertThat(savedMember.getEmail()).isEqualTo(email);
        assertThat(savedMember.getNickname()).isEqualTo(nickname);
        assertThat(savedMember.getPassword()).isEqualTo("encodedPassword");

        assertThat(savedMember.getProfileImageKey()).isEqualTo("public/default.png");

        verify(memberRepository).save(any(Member.class));
    }
}