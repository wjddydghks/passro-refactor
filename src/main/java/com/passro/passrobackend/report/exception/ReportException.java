package com.passro.passrobackend.report.exception;

import com.passro.passrobackend.global.exception.APIException;
import com.passro.passrobackend.report.exception.code.ReportErrorCode;

public class ReportException extends APIException {
    public ReportException(ReportErrorCode code) {
        super(code);
    }
}
