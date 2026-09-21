package com.passro.passrobackend.file.exception.code;

import com.passro.passrobackend.global.code.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
@Getter
public enum FileErrorCode implements BaseErrorCode {
    INVALID_IMAGE_FORMAT(HttpStatus.BAD_REQUEST, "FILE400_1", "지원하지 않는 이미지 형식입니다."),
    INVALID_FILE_SIZE(HttpStatus.BAD_REQUEST, "FILE400_2", "파일 크기는 1바이트 이상 10MB 이하여야 합니다."),
    INVALID_FILE_NAME(HttpStatus.BAD_REQUEST, "FILE400_3", "유효한 이미지 파일 이름이 필요합니다."),
    FILE_NOT_FOUND(HttpStatus.NOT_FOUND, "FILE404_1", "파일을 찾을 수 없습니다."),
    FILE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "FILE500_1", "파일 업로드 주소를 가져오는 데 실패했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
