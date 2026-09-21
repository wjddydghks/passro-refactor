package com.passro.passrobackend.chat.code;

import com.passro.passrobackend.global.code.BaseSuccessCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ChatSuccessCode implements BaseSuccessCode {

    OK(HttpStatus.OK,
            "CHAT200_1",
            "요청 성공."),

    CREATED(HttpStatus.CREATED,
            "CHAT201_1",
            "메시지 전송 성공."),

    LEFT(HttpStatus.OK,
            "CHAT200_2",
            "채팅방 나가기 성공.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
