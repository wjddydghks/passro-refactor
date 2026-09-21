package com.passro.passrobackend.market.code;

import com.passro.passrobackend.global.code.BaseSuccessCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum MarketSuccessCode implements BaseSuccessCode {
    OK(HttpStatus.OK, "MARKET200_1", "요청 성공."),
    CREATED(HttpStatus.CREATED, "MARKET201_1", "상품 등록 성공.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
