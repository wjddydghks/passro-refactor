package com.passro.passrobackend.point.exception.code;

import com.passro.passrobackend.global.code.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum PointErrorCode implements BaseErrorCode {
    INVALID_AMOUNT(HttpStatus.BAD_REQUEST, "POINT400_1", "포인트는 0보다 커야 합니다."),
    INSUFFICIENT_BALANCE(HttpStatus.CONFLICT, "POINT409_1", "보유 포인트가 부족합니다."),
    ALREADY_PROCESSED(HttpStatus.CONFLICT, "POINT409_2", "이미 처리된 포인트 거래입니다."),
    PAYMENT_NOT_FOUND(HttpStatus.CONFLICT, "POINT409_3", "배송 결제 내역을 찾을 수 없습니다."),
    ACCOUNT_NOT_FOUND(HttpStatus.NOT_FOUND, "POINT404_1", "포인트 계정을 찾을 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
