package com.passro.passrobackend.report.code;

import com.passro.passrobackend.global.code.BaseSuccessCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ReportSuccessCode implements BaseSuccessCode {

    REPORT_CREATED(HttpStatus.CREATED, "REPORT201_1", "신고 작성 성공"),
    MY_REPORTS_FOUND(HttpStatus.OK, "REPORT200_1", "내 신고 목록 조회 성공"),
    REPORT_STATUS_UPDATED(HttpStatus.OK, "REPORT200_2", "신고 상태 변경 성공");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
