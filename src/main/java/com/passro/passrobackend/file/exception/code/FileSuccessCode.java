package com.passro.passrobackend.file.exception.code;

import com.passro.passrobackend.global.code.BaseSuccessCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum FileSuccessCode implements BaseSuccessCode {
    OK(HttpStatus.OK,
            "FILE200_1",
            "요청 성공.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
