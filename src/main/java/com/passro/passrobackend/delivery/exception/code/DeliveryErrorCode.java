package com.passro.passrobackend.delivery.exception.code;

import com.passro.passrobackend.global.code.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum DeliveryErrorCode implements BaseErrorCode {

    NOT_FOUND(HttpStatus.NOT_FOUND,
            "DELIVERY404_1",
            "해당 배송을 찾을 수 없습니다."),
    PLACE_NOT_FOUND(HttpStatus.NOT_FOUND,
            "DELIVERY404_2",
            "해당 장소를 찾을 수 없습니다."),
    FORBIDDEN_ACCESS(HttpStatus.FORBIDDEN,
            "DELIVERY403_1",
            "해당 배송에 대한 접근 권한이 없습니다."),
    SELF_DELIVERY_NOT_ALLOWED(HttpStatus.FORBIDDEN,
            "DELIVERY403_2",
            "본인이 요청한 배송은 수락할 수 없습니다."),
    CANNOT_CANCEL(HttpStatus.BAD_REQUEST,
            "DELIVERY400_1",
            "매칭이 진행된 배송은 취소할 수 없습니다."),
    INVALID_STATUS_FOR_COMPLETION(HttpStatus.BAD_REQUEST,
            "DELIVERY400_2",
            "배송 완료 처리를 할 수 없는 상태입니다."),
    INVALID_STATUS_TRANSITION(HttpStatus.BAD_REQUEST,
            "DELIVERY400_3",
            "현재 배송 상태에서는 요청한 상태 변경을 수행할 수 없습니다."),
    SAME_ORIGIN_DESTINATION_NOT_ALLOWED(HttpStatus.BAD_REQUEST,
            "DELIVERY400_4",
            "출발역과 도착역은 같을 수 없습니다."),
    DELIVERY_POINT_NOT_FOUND(HttpStatus.NOT_FOUND,
            "DELIVERY404_3",
            "해당 배송의 결제 포인트 정보를 찾을 수 없습니다."),
    SHIPPER_NOT_ASSIGNED(HttpStatus.NOT_FOUND,
            "DELIVERY404_4",
            "해당 배송에 매칭된 배송기사가 없습니다."),
    SHIPPER_ROUTE_NOT_FOUND(HttpStatus.NOT_FOUND,
            "DELIVERY404_5",
            "매칭된 배송기사의 통학 경로를 찾을 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
