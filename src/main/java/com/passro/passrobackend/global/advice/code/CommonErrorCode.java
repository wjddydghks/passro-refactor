package com.passro.passrobackend.global.advice.code;

import com.passro.passrobackend.global.code.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
@Getter
public enum CommonErrorCode implements BaseErrorCode {

    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "COMMON400", "잘못된 요청"),
    MISSING_REQUEST_PARAMETER(HttpStatus.BAD_REQUEST, "COMMON400", "요청 파라미터 누락"),
    INVALID_REQUEST_PARAMETER_TYPE(HttpStatus.BAD_REQUEST, "COMMON400", "잘못된 요청 파라미터 타입"),
    INVALID_REQUEST_BODY(HttpStatus.BAD_REQUEST, "COMMON400", "요청 페이로드 파싱 실패; tip: JSON무결성 확인해보세요!"),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "COMMON404", "요청한 리소스를 찾을 수 없음"),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "COMMON405", "지원되지 않는 HTTP 메서드"),
    DATA_INTEGRITY_VIOLATION(HttpStatus.CONFLICT, "COMMON409", "이미 존재하는 데이터입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON500", "예상치 못한 오류");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
