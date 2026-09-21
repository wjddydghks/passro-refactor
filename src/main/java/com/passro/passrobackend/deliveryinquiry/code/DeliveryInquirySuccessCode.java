package com.passro.passrobackend.deliveryinquiry.code;

import com.passro.passrobackend.global.code.BaseSuccessCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@AllArgsConstructor
@Getter
public enum DeliveryInquirySuccessCode implements BaseSuccessCode {

    OK(HttpStatus.OK,
            "DELIVERY_INQUIRY200_1",
            "배송 문의 조회 성공."),

    CREATED(HttpStatus.CREATED,
            "DELIVERY_INQUIRY201_1",
            "배송 문의 등록 성공.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
