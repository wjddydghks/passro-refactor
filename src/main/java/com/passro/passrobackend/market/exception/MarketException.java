package com.passro.passrobackend.market.exception;

import com.passro.passrobackend.global.code.BaseErrorCode;
import com.passro.passrobackend.global.exception.APIException;

public class MarketException extends APIException {

    public MarketException(BaseErrorCode code) {
        super(code);
    }
}
