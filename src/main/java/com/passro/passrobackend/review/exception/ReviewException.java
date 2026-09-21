package com.passro.passrobackend.review.exception;

import com.passro.passrobackend.global.code.BaseErrorCode;
import com.passro.passrobackend.global.exception.APIException;

public class ReviewException extends APIException {
    public ReviewException(BaseErrorCode code) {
        super(code);
    }
}
