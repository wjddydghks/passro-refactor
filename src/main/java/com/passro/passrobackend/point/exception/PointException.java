package com.passro.passrobackend.point.exception;

import com.passro.passrobackend.global.code.BaseErrorCode;
import com.passro.passrobackend.global.exception.APIException;

public class PointException extends APIException {
    public PointException(BaseErrorCode code) {
        super(code);
    }
}
