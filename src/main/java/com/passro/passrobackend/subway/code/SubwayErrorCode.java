package com.passro.passrobackend.subway.code;

import com.passro.passrobackend.global.code.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum SubwayErrorCode implements BaseErrorCode {
    PLACE_NOT_FOUND(HttpStatus.NOT_FOUND, "SUBWAY404_1", "지하철역 Place를 찾을 수 없습니다."),
    ROUTE_NOT_FOUND(HttpStatus.NOT_FOUND, "SUBWAY404_2", "지하철 경로를 찾을 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
