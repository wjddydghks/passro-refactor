package com.passro.passrobackend.shipper.code;

import com.passro.passrobackend.global.code.BaseSuccessCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@AllArgsConstructor
@Getter
public enum ShipperSuccessCode implements BaseSuccessCode {
    OK(HttpStatus.OK,
            "SHIPPER200_1",
            "요청 성공.");

    private final HttpStatus status;
    private final String code;
    private final String message;

}
