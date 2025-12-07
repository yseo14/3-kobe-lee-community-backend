# ⛹️‍♂️ Locker Room 🏀
## 🏆 소개
Locker Room: 농구인들을 위한 오프 코트(Off-Court) 커뮤니티 <p>
땀 흘린 뒤 라커룸에서 나누는 대화처럼, 농구인들이 경기 전후의 설렘과 정보를 나누는 공간입니다. 플레이어들을 위한 정보 공유부터 가벼운 잡담까지, 코트 밖에서도 이어지는 커뮤니티 문화를 지향합니다.

## 🛠 기술 스택

### Backend
- **Java 21**
- **Spring Boot 3.5.6**
- **Spring Data JPA**
- **Spring Security**
- **QueryDSL 5.1.0**

### Database & Cache
- **MySQL 8.0**
- **Redis(AWS ElastiCache Valkey)**

### Infra
- **Docker & Docker Compose**
- **AWS RDS**
- **AWS ElatiCache Valkey**
- **AWS S3** (이미지 및 배포 스크립트 저장)

## ✨ 주요 기능

### 인증/인가
- 회원가입 (이메일, 닉네임 중복 체크)
- 로그인/로그아웃
- JWT 기반 인증
- Refresh Token 자동 갱신

### 회원 관리
- 회원 정보 조회
- 프로필 수정
- 비밀번호 변경
- 프로필 이미지 업로드 (S3)

### 게시글 관리
- 게시글 작성/수정/삭제
- 게시글 목록 조회 (커서 기반 페이징)
- 게시글 상세 조회
- 게시글 좋아요
- 이미지 업로드 (S3)

### 댓글 관리
- 댓글 작성/수정/삭제
- 댓글 목록 조회 (커서 기반 페이징)

## 🌳 패키지 구조
<details>
 <summary>패키지 구조 확인하기</summary>
 
 ```
📁
src/main/java/com/example/community/
├── auth/              # 인증/인가
│   ├── api/          # AuthController
│   ├── application/   # AuthService
│   ├── jwt/          # JWT 유틸리티
│   └── Exception/     # 인증 예외
├── member/            # 회원 관리
│   ├── api/          # MemberController, DTO
│   ├── application/   # MemberService
│   ├── domain/        # Member 엔티티
│   └── repository/    # MemberRepository
├── Post/              # 게시글 관리
│   ├── api/          # PostController, DTO
│   ├── application/   # PostService
│   ├── domain/        # Post 엔티티
│   └── repository/    # PostRepository
├── comment/           # 댓글 관리
│   ├── api/          # CommentController, DTO
│   ├── application/   # CommentService
│   ├── domain/        # Comment 엔티티
│   └── repository/      # CommentRepository
├── image/             # 이미지 관리
├── postImage/         # 게시글 이미지
├── memberPostLike/    # 게시글 좋아요
├── policy/            # 정책 페이지
└── global/            # 공통 설정
    ├── config/        # SecurityConfig, RedisConfig 등
    ├── exception/     # 전역 예외 처리
    ├── response/      # API 응답 형식
    └── redis/         # Redis 유틸리티

 ```

</details>

## 🔗 ERD
<img width="1208" height="753" alt="community_erd" src="https://github.com/user-attachments/assets/f9049605-3ce5-4d43-ae0c-96503b151c40" />

## 👷🏻‍♂️ Infra Architecture
<img width="9520" height="5240" alt="community_architecture" src="https://github.com/user-attachments/assets/1ad6a3eb-4306-47aa-b9eb-8c6e2fa4da95" />

