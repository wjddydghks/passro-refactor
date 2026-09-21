package com.passro.passrobackend.delivery.enums;

public enum DeliveryState {
    // 매칭 전 상태
    WAIT,

    // 매칭 된 상태
    MATCHED,

    // 물품 인수 후 배송 중 상태
    DELIVERING,

    // 배송 후 검수 요청 상태
    CONFIRM_REQUESTED,

    // 검수 완료, 배달 끝
    DELIVERED,

    // 취소 상태
    CANCEL
}
