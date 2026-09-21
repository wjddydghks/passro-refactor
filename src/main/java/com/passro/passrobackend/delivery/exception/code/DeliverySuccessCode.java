package com.passro.passrobackend.delivery.exception.code;

import com.passro.passrobackend.global.code.BaseSuccessCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@AllArgsConstructor
@Getter
public enum DeliverySuccessCode implements BaseSuccessCode {
    OK(HttpStatus.OK,
            "DELIVERY200_1",
            "요청 성공.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
