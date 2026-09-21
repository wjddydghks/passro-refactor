package com.passro.passrobackend.account.exception.code;

import com.passro.passrobackend.global.code.BaseErrorCode;
import com.passro.passrobackend.global.code.BaseSuccessCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
@Getter
public enum AccountErrorCode implements BaseErrorCode {

    NOT_FOUND(HttpStatus.NOT_FOUND,
            "ACCOUNT404_1",
            "해당 계정을 찾을 수 없습니다."),

    NOT_FOUND_SUBWAY(HttpStatus.NOT_FOUND,
            "Account404_2",
            "지하철을 찾을 수 없습니다."),

    MAIL_CODE_EXPIRED(HttpStatus.BAD_REQUEST,
            "ACCOUNT400_1",
            "인증 코드가 만료되었거나 존재하지 않습니다."),

    MAIL_CODE_MISMATCH(HttpStatus.BAD_REQUEST,
            "ACCOUNT400_2",
            "인증 코드가 일치하지 않습니다."),

    MAIL_NOT_CONFIRM(HttpStatus.BAD_REQUEST,
            "ACCOUNT400_3",
            "인증되지 않은 이메일입니다."),

    DUPLICATE_MAIL(HttpStatus.BAD_REQUEST,
            "ACCOUNT400_4",
            "사용 중인 이메일입니다."),

    DUPLICATE_NICKNAME(HttpStatus.BAD_REQUEST,
            "ACCOUNT400_6",
            "사용 중인 닉네임입니다."),

    DUPLICATE_PHONE_NUMBER(HttpStatus.BAD_REQUEST,
            "ACCOUNT400_7",
            "사용 중인 전화번호입니다."),

    INVALID_MAIL_DOMAIN(HttpStatus.BAD_REQUEST,
            "ACCOUNT400_8",
            "학생용 이메일이 아닙니다."),

    SAME_PASSWORD(HttpStatus.BAD_REQUEST,
            "ACCOUNT400_9",
            "현재 비밀번호와 일치합니다."),

    NOT_SAME_PASSWORD(HttpStatus.BAD_REQUEST,
            "ACCOUNT400_9",
            "현재 비밀번호와 일치하지않습니다."),

    TOO_FAST(HttpStatus.TOO_MANY_REQUESTS,
            "ACCOUNT429_1",
            "잠시 후 다시 시도해주세요."),

    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED,
            "ACCOUNT401_1",
            "이메일 또는 비밀번호가 일치하지 않습니다."),

    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED,
            "ACCOUNT401_2",
            "유효하지 않은 리프레시 토큰입니다."),

    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED,
            "ACCOUNT401_3",
            "토큰이 만료되었습니다."),

    ;


    private final HttpStatus status;
    private final String code;
    private final String message;
}
