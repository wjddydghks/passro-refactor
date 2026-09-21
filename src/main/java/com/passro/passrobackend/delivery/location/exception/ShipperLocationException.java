package com.passro.passrobackend.delivery.location.exception;

import com.passro.passrobackend.delivery.location.exception.code.ShipperLocationErrorCode;
import com.passro.passrobackend.global.exception.APIException;

public class ShipperLocationException extends APIException {
    public ShipperLocationException(ShipperLocationErrorCode code) {
        super(code);
    }
}
