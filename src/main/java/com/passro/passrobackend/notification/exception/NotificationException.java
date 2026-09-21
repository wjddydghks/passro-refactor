package com.passro.passrobackend.notification.exception;

import com.passro.passrobackend.global.code.BaseErrorCode;
import com.passro.passrobackend.global.exception.APIException;

public class NotificationException extends APIException {
    public NotificationException(BaseErrorCode code) {
        super(code);
    }
}
