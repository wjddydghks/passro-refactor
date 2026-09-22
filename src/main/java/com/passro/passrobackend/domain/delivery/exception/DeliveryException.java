package com.passro.passrobackend.domain.delivery.exception;

import com.passro.passrobackend.global.code.BaseErrorCode;
import com.passro.passrobackend.global.exception.APIException;

public class DeliveryException extends APIException {
    public DeliveryException(BaseErrorCode code) {
        super(code);
    }
}
