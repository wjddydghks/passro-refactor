package com.passro.passrobackend.notification.code;

import com.passro.passrobackend.global.code.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@AllArgsConstructor
@Getter
public enum NotificationErrorCode implements BaseErrorCode {

    NOT_FOUND(HttpStatus.NOT_FOUND,
            "NOTIFICATION404_1",
            "해당 알림을 찾을 수 없습니다."),

    FORBIDDEN(HttpStatus.FORBIDDEN,
            "NOTIFICATION403_1",
            "본인의 알림만 조작할 수 있습니다."),

    INVALID_PAGINATION(HttpStatus.BAD_REQUEST,
            "NOTIFICATION400_1",
            "잘못된 페이지네이션 요청입니다. page는 0 이상, size는 1 이상 100 이하여야 합니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
