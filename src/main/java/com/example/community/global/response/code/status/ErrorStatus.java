package com.example.community.global.response.code.status;

import com.example.community.global.response.code.BaseErrorCode;
import com.example.community.global.response.code.ErrorReasonDto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorStatus implements BaseErrorCode {

    // 가장 일반적인 응답
    _INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON500", "서버 에러, 관리자에게 문의 바랍니다."),
    _BAD_REQUEST(HttpStatus.BAD_REQUEST, "COMMON400", "잘못된 요청입니다."),
    _UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "COMMON401", "인증이 필요합니다."),
    _FORBIDDEN(HttpStatus.FORBIDDEN, "COMMON403", "금지된 요청입니다."),
    _EMPTY_FIELD(HttpStatus.NO_CONTENT, "COMMON404", "입력 값이 누락되었습니다."),
    _UNAUTHORIZED_ACCESS(HttpStatus.UNAUTHORIZED, "COMMON405", "권한이 없습니다."),

    // 400 BAD REQUEST
    PASSWORD_MISMATCH(HttpStatus.BAD_REQUEST, "400_001", "비밀번호와 비밀번호 확인이 일치하지 않습니다."),
    INVALID_CURRENT_PASSWORD(HttpStatus.BAD_REQUEST, "400_002", "현재 비밀번호가 일치하지 않습니다."),
    SAME_PASSWORD(HttpStatus.BAD_REQUEST, "400_003", "새 비밀번호는 현재 비밀번호와 다르게 설정해야 합니다."),
    INVALID_TOKEN_FORMAT(HttpStatus.BAD_REQUEST, "400_004", "유효하지 않은 토큰 형식입니다."),

    // 401 UNAUTHORIZED
    LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "401_001", "로그인에 실패하였습니다."),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "401_002", "토큰이 만료되었습니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "401_003", "토큰이 유효하지 않습니다."),
    UNSUPPORTED_TOKEN(HttpStatus.UNAUTHORIZED, "401_004", "지원하지 않는 JWT 토큰입니다."),
    EMPTY_TOKEN(HttpStatus.UNAUTHORIZED, "401_005", "JWT 토큰이 비어 있습니다."),
    BLACKLISTED_TOKEN(HttpStatus.UNAUTHORIZED, "401_006", "블랙리스트에 등록된 토큰입니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "401_007", "유효하지 않은 리프레시 토큰입니다."),

    // 403 FORBIDDEN
    NO_PERMISSION(HttpStatus.FORBIDDEN, "403_001", "해당 작업을 수행할 권한이 없습니다."),

    // 404 NOT FOUND
    DEFAULT_IMAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "404_001", "기본 이미지가 존재하지 않습니다."),
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "404_002", "사용자 정보가 존재하지 않습니다."),
    IMAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "404_003", "이미지가 존재하지 않습니다."),
    POST_NOT_FOUND(HttpStatus.NOT_FOUND, "404_004", "게시글이 존재하지 않습니다."),
    COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "404_005", "댓글이 존재하지 않습니다."),
    TOKEN_NOT_FOUND(HttpStatus.NOT_FOUND, "404_006", "토큰이 존재하지 않습니다."),


    // 409 CONFLICT
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "409_001", "이미 사용 중인 이메일입니다."),
    DUPLICATE_NICKNAME(HttpStatus.CONFLICT, "409_002", "이미 사용 중인 닉네임입니다."),
    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    @Override
    public ErrorReasonDto getReason() {
        return ErrorReasonDto.builder()
                .message(message)
                .code(code)
                .isSuccess(false)
                .build();
    }

    @Override
    public ErrorReasonDto getReasonHttpStatus() {
        return ErrorReasonDto.builder()
                .message(message)
                .code(code)
                .isSuccess(false)
                .httpStatus(httpStatus)
                .build();
    }
}
