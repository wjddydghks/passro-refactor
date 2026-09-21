package com.passro.passrobackend.chat.exception.code;

import com.passro.passrobackend.global.code.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ChatErrorCode implements BaseErrorCode {

    DELIVERY_NOT_FOUND(HttpStatus.NOT_FOUND,
            "CHAT404_1",
            "해당 배송을 찾을 수 없습니다."),
    FORBIDDEN_ACCESS(HttpStatus.FORBIDDEN,
            "CHAT403_1",
            "해당 채팅에 접근 권한이 없습니다."),
    CHAT_NOT_AVAILABLE(HttpStatus.BAD_REQUEST,
            "CHAT400_1",
            "매칭이 완료된 배송건만 채팅이 가능합니다."),
    CHAT_ROOM_NOT_FOUND(HttpStatus.NOT_FOUND,
            "CHAT404_2",
            "해당 채팅방을 찾을 수 없습니다."),
    CHAT_ROOM_ALREADY_LEFT(HttpStatus.FORBIDDEN,
            "CHAT403_2",
            "이미 나간 채팅방에는 접근할 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
