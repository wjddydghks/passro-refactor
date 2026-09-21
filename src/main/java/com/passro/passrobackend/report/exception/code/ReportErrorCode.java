package com.passro.passrobackend.report.exception.code;

import com.passro.passrobackend.global.code.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ReportErrorCode implements BaseErrorCode {

    REPORT_DELIVERY_NOT_FOUND(HttpStatus.NOT_FOUND, "REPORT404_1", "해당 배송 건을 찾을 수 없습니다."),
    REPORT_CHAT_MESSAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "REPORT404_2", "해당 채팅 메시지를 찾을 수 없습니다."),
    REPORT_ACCOUNT_NOT_FOUND(HttpStatus.NOT_FOUND, "REPORT404_3", "해당 사용자를 찾을 수 없습니다."),
    REPORT_NOT_FOUND(HttpStatus.NOT_FOUND, "REPORT404_4", "해당 신고를 찾을 수 없습니다."),

    REPORT_FORBIDDEN(HttpStatus.FORBIDDEN, "REPORT403_1", "신고 권한이 없습니다."),
    REPORT_SELF_ACCOUNT_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "REPORT400_1", "자기 자신은 신고할 수 없습니다."),
    REPORT_SELF_MESSAGE_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "REPORT400_2", "본인이 작성한 메시지는 신고할 수 없습니다."),
    REPORT_ALREADY_EXISTS(HttpStatus.BAD_REQUEST, "REPORT400_3", "동일 대상에 대한 신고는 한 번만 작성할 수 있습니다."),

    INVALID_REPORT_TARGET_TYPE(HttpStatus.BAD_REQUEST, "REPORT400_4", "유효하지 않은 신고 대상 유형입니다."),
    INVALID_REPORT_REASON(HttpStatus.BAD_REQUEST, "REPORT400_5", "유효하지 않은 신고 사유입니다."),
    INVALID_REPORT_DETAIL(HttpStatus.BAD_REQUEST, "REPORT400_6", "상세 내용이 유효하지 않습니다."),
    INVALID_REPORT_IMAGE_COUNT(HttpStatus.BAD_REQUEST, "REPORT400_7", "이미지는 최대 5장까지 첨부할 수 있습니다."),
    INVALID_REPORT_DELIVERY_ID(HttpStatus.BAD_REQUEST, "REPORT400_8", "deliveryId는 필수입니다."),
    INVALID_REPORT_CHAT_MESSAGE_ID(HttpStatus.BAD_REQUEST, "REPORT400_9", "chatMessageId는 필수입니다."),
    INVALID_REPORTED_ACCOUNT_ID(HttpStatus.BAD_REQUEST, "REPORT400_10", "reportedAccountId는 필수입니다."),
    INVALID_REPORT_OTHER_DETAIL(HttpStatus.BAD_REQUEST, "REPORT400_11", "신고 사유가 OTHER인 경우 상세 내용은 필수입니다."),
    INVALID_REPORT_STATUS(HttpStatus.BAD_REQUEST, "REPORT400_12", "변경할 신고 상태가 유효하지 않습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
