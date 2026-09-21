package com.passro.passrobackend.notification.code;

import com.passro.passrobackend.global.code.BaseSuccessCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@AllArgsConstructor
@Getter
public enum NotificationSuccessCode implements BaseSuccessCode {

    OK(HttpStatus.OK,
            "NOTIFICATION200_1",
            "알림 조회 성공."),

    UNREAD_COUNT_OK(HttpStatus.OK,
            "NOTIFICATION200_2",
            "미확인 알림 수 조회 성공."),

    READ_OK(HttpStatus.OK,
            "NOTIFICATION200_3",
            "알림 확인 처리 성공."),

    DELETED(HttpStatus.OK,
            "NOTIFICATION200_4",
            "알림 삭제 성공."),

    READ_ALL_OK(HttpStatus.OK,
            "NOTIFICATION200_5",
            "전체 알림 확인 처리 성공."),

    DELETED_ALL(HttpStatus.OK,
            "NOTIFICATION200_6",
            "전체 알림 삭제 성공.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
