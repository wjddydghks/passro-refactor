package com.passro.passrobackend.subway.code;

import com.passro.passrobackend.global.code.BaseSuccessCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum SubwaySuccessCode implements BaseSuccessCode {
    ROUTE_OK(HttpStatus.OK, "SUBWAY200_1", "최단 경로 탐색 성공."),
    STATION_SEARCH_OK(HttpStatus.OK, "SUBWAY200_2", "지하철역 검색 성공.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
