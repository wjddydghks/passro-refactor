package com.passro.passrobackend.file.exception;

import com.passro.passrobackend.file.exception.code.FileErrorCode;
import com.passro.passrobackend.global.exception.APIException;

public class FileException extends APIException {
    public FileException(FileErrorCode code) {
        super(code);
    }
}
