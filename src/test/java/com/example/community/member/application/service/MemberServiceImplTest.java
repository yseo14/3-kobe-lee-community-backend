package com.example.community.member.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.example.community.auth.jwt.JwtUtils;
import com.example.community.global.config.AppProperties;
import com.example.community.image.domain.Image;
import com.example.community.image.repository.ImageRepository;
import com.example.community.member.api.dto.SignUpRequest;
import com.example.community.member.domain.Member;
import com.example.community.member.repository.MemberRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.password.PasswordEncoder;

class MemberServiceImplTest {
    @InjectMocks
    private MemberServiceImpl memberService; // 테스트할 대상 (Service)

    @Mock
    private MemberRepository memberRepository; // 가짜 저장소
    @Mock
    private ImageRepository imageRepository;   // 가짜 이미지 저장소
    @Mock
    private PasswordEncoder passwordEncoder;   // 가짜 암호화 도구
    @Mock
    private AppProperties appProperties;       // 가짜 설정값
    @Mock
    private JwtUtils jwtUtils;                 // 가짜 JWT 도구

    @BeforeEach
    void setUp() {
        // Mock 객체 초기화 (NPE 방지용 안전장치)
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @DisplayName("회원가입 성공 테스트")
    void signUp_success() {
        // given (준비 단계)
        String email = "test@example.com";
        String password = "password123";
        String nickname = "tester";
        Long defaultImageId = 1L;

        // 회원가입 요청 DTO 생성
        // 기존: new SignUpRequest(email, password, password, nickname, null); -> 순서 불일치로 인한 Mismatch 에러
        // 수정: new SignUpRequest(email, nickname, password, password, null); -> (이메일, 닉네임, 비번, 비번확인, 이미지) 순서로 추정
        SignUpRequest request = new SignUpRequest(email, nickname, password, password, null);

        // 기본 프로필 이미지 생성 (Mock) - Image 엔티티 필드 변경 반영
        Image defaultImage = Image.builder()
                .id(defaultImageId)
                .objectKey("images/default-profile.png")
                .fileSize(1024)
                .mimeType("image/png")
                .width(100)
                .height(100)
                .isUsed(true)
                .build();

        // Mock 객체들의 행동 정의 (Stubbing)
        // 1. 이메일 중복 체크 시 false 반환 (중복 없음)
        given(memberRepository.existsByEmail(email)).willReturn(false);
        // 2. 닉네임 중복 체크 시 false 반환 (중복 없음)
        given(memberRepository.existsByNickname(nickname)).willReturn(false);
        // 3. 비밀번호 암호화 시 특정 문자열 반환
        given(passwordEncoder.encode(password)).willReturn("encodedPassword");
        // 4. 기본 이미지 ID 조회 시 1L 반환
        given(appProperties.getDefaultProfileImageId()).willReturn(defaultImageId);
        // 5. 이미지 ID로 조회 시 이미지 객체 반환
        given(imageRepository.findById(defaultImageId)).willReturn(Optional.of(defaultImage));
        // 6. 회원 저장 시 저장된 Member 객체 반환 (여기서는 단순히 호출 여부만 중요할 수도 있음)
        given(memberRepository.save(any(Member.class))).willAnswer(invocation -> invocation.getArgument(0));


        // when (실행 단계)
        Member savedMember = memberService.signUp(request);

        // then (검증 단계)
        assertThat(savedMember).isNotNull(); // 결과가 null이 아니어야 함
        assertThat(savedMember.getEmail()).isEqualTo(email); // 이메일이 일치해야 함
        assertThat(savedMember.getNickname()).isEqualTo(nickname); // 닉네임이 일치해야 함
        assertThat(savedMember.getPassword()).isEqualTo("encodedPassword"); // 비밀번호가 암호화되어야 함
        assertThat(savedMember.getProfileImage()).isEqualTo(defaultImage); // 프로필 이미지가 설정되어야 함

        // verify: 실제로 save 메서드가 호출되었는지 확인
        verify(memberRepository).save(any(Member.class));
    }
}