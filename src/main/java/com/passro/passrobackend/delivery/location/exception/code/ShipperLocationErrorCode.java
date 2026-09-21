package com.passro.passrobackend.delivery.location.exception.code;

import com.passro.passrobackend.global.code.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ShipperLocationErrorCode implements BaseErrorCode {
    NOT_FOUND(HttpStatus.NOT_FOUND,
            "SHIPPER_LOCATION404_1",
            "배송기사의 현재 위치를 찾을 수 없습니다."),
    UPDATE_NOT_ALLOWED(HttpStatus.FORBIDDEN,
            "SHIPPER_LOCATION403_1",
            "배송 중인 배송기사만 현재 위치를 갱신할 수 있습니다."),
    TRACKING_NOT_AVAILABLE(HttpStatus.BAD_REQUEST,
            "SHIPPER_LOCATION400_1",
            "배송 중인 상태에서만 배송기사 위치를 조회할 수 있습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
