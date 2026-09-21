package com.passro.passrobackend.account.exception;

import com.passro.passrobackend.global.code.BaseErrorCode;
import com.passro.passrobackend.global.exception.APIException;

public class AccountException extends APIException {
    public AccountException(BaseErrorCode code) {
        super(code);
    }
}
