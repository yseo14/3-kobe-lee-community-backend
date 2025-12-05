package com.example.community.global.response.code.status;

import com.example.community.global.response.code.BaseCode;
import com.example.community.global.response.code.ReasonDto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum SuccessStatus implements BaseCode {

    // 일반적인 응답
    _OK(HttpStatus.OK, "COMMON200", "성공입니다."),

    EMAIL_AVAILABLE(HttpStatus.OK, "200_001", "사용 가능한 이메일입니다."),
    EMAIL_DUPLICATED(HttpStatus.OK, "200_002", "이미 사용 중인 이메일입니다."),
    NICKNAME_AVAILABLE(HttpStatus.OK, "200_003", "사용 가능한 닉네임입니다."),
    NICKNAME_DUPLICATED(HttpStatus.OK, "200_004", "이미 사용 중인 닉네임입니다."),
    SIGNUP_SUCCESS(HttpStatus.OK, "200_005", "회원가입이 완료되었습니다."),
    LOGIN_SUCCESS(HttpStatus.OK,"200_006", "로그인에 성공했습니다."),
    LOGOUT_SUCCESS(HttpStatus.OK,"200_007", "로그아웃에 성공했습니다."),
    MEMBER_INFO_FOUND(HttpStatus.OK,"200_008", "사용자 정보 조회에 성공했습니다."),
    MEMBER_INFO_UPDATE(HttpStatus.OK,"200_009", "사용자 정보 수정에 성공했습니다."),
    PASSWORD_UPDATE(HttpStatus.OK,"200_010", "비밀번호 수정에 성공했습니다."),
    DELETE_MEMBER(HttpStatus.OK,"200_011", "탈퇴 처리되었습니다."),
    CREATE_POST(HttpStatus.OK,"200_012", "게시글이 생성되었습니다."),
    DELETE_POST(HttpStatus.OK,"200_013", "게시글이 삭제되었습니다."),
    UPDATE_POST(HttpStatus.OK,"200_014", "게시글이 수정되었습니다."),
    GET_POST_LIST(HttpStatus.OK,"200_015", "게시글 목록을 조회하였습니다."),
    GET_POST(HttpStatus.OK,"200_016", "게시글을 조회하였습니다."),
    CREATE_COMMENT(HttpStatus.OK,"200_017", "댓글을 생성하였습니다."),
    DELETE_COMMENT(HttpStatus.OK,"200_018", "댓글을 삭제하였습니다."),
    UPDATE_COMMENT(HttpStatus.OK,"200_019", "댓글을 수정하였습니다."),
    GET_COMMENT_LIST(HttpStatus.OK,"200_020", "댓글 목록을 조회하였습니다."),
    REFRESH_SUCCESS(HttpStatus.OK,"200_021", "토큰을 재발급하였습니다."),
    INCREMENT_VIEW_COUNT(HttpStatus.OK,"200_022", "조회수가 증가되었습니다."),
    LIKE_POST(HttpStatus.OK,"200_023", "좋아요가 추가되었습니다."),
    UNLIKE_POST(HttpStatus.OK,"200_024", "좋아요가 취소되었습니다."),

    ;


    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    @Override
    public ReasonDto getReason() {
        return ReasonDto.builder()
                .message(message)
                .code(code)
                .isSuccess(true)
                .build();
    }

    @Override
    public ReasonDto getReasonHttpStatus() {
        return ReasonDto.builder()
                .message(message)
                .code(code)
                .isSuccess(true)
                .httpStatus(httpStatus)
                .build();
    }
}
