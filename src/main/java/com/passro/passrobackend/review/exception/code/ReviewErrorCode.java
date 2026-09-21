package com.passro.passrobackend.review.exception.code;

import com.passro.passrobackend.global.code.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ReviewErrorCode implements BaseErrorCode {

    REVIEW_DELIVERY_NOT_FOUND(HttpStatus.NOT_FOUND, "REVIEW404_1", "해당 배송 건을 찾을 수 없습니다."),
    REVIEW_ACCOUNT_NOT_FOUND(HttpStatus.NOT_FOUND, "REVIEW404_2", "해당 사용자를 찾을 수 없습니다."),
    REVIEW_FORBIDDEN(HttpStatus.FORBIDDEN, "REVIEW403_1", "해당 배송 건의 발송자만 리뷰를 작성할 수 있습니다."),
    REVIEW_DELIVERY_NOT_COMPLETED(HttpStatus.BAD_REQUEST, "REVIEW400_1", "배송 완료 건에 대해서만 리뷰를 작성할 수 있습니다."),
    REVIEW_ALREADY_EXISTS(HttpStatus.BAD_REQUEST, "REVIEW400_2", "해당 배송 건에는 이미 리뷰가 작성되었습니다."),
    INVALID_REVIEW_RATING(HttpStatus.BAD_REQUEST, "REVIEW400_3", "평점은 1점 이상 5점 이하로 입력해야 합니다."),
    INVALID_REVIEW_DELIVERY_ID(HttpStatus.BAD_REQUEST, "REVIEW400_4", "deliveryId는 필수입니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
