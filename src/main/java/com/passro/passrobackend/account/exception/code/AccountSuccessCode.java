package com.passro.passrobackend.account.exception.code;

import com.passro.passrobackend.global.code.BaseSuccessCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
@Getter
public enum AccountSuccessCode implements BaseSuccessCode {

    OK(HttpStatus.OK,
            "ACCOUNT200_1",
            "요청 성공.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
