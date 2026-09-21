package com.passro.passrobackend.review.code;

import com.passro.passrobackend.global.code.BaseSuccessCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ReviewSuccessCode implements BaseSuccessCode {

    REVIEW_CREATED(HttpStatus.CREATED, "REVIEW201_1", "리뷰 작성 성공"),
    REVIEW_AVERAGE_RATING_FOUND(HttpStatus.OK, "REVIEW200_1", "평균 평점 조회 성공");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
