package com.passro.passrobackend.global.exception;

import com.passro.passrobackend.global.code.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class APIException extends RuntimeException {
    private final BaseErrorCode code;
}
