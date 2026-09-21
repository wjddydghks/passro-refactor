package com.passro.passrobackend.chat.exception;

import com.passro.passrobackend.global.code.BaseErrorCode;
import com.passro.passrobackend.global.exception.APIException;

public class ChatException extends APIException {
    public ChatException(BaseErrorCode code) {
        super(code);
    }
}