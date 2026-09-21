package com.passro.passrobackend.sender.code;

import com.passro.passrobackend.global.code.BaseSuccessCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@AllArgsConstructor
@Getter
public enum SenderSuccessCode implements BaseSuccessCode {

    OK(HttpStatus.OK,
            "SENDER200_1",
            "요청 성공."),

    CREATED(HttpStatus.CREATED,
            "SENDER201_1",
            "생성 성공.");

    private final HttpStatus status;
    private final String code;
    private final String message;

}
