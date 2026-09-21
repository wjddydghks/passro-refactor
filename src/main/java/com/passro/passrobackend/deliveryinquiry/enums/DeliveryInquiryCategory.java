package com.passro.passrobackend.deliveryinquiry.enums;

/**
 * 배송 문의 카테고리 (특정 배송 건에 대한 문의)
 */
public enum DeliveryInquiryCategory {
    // 배송 지연
    DELAY,

    // 파손
    DAMAGE,

    // 분실
    LOST,

    // 오배송
    WRONG_DELIVERY,

    // 요금/포인트
    POINT,

    // 기타
    ETC
}
