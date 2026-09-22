package com.passro.passrobackend.global.advice;

import com.passro.passrobackend.global.advice.code.CommonErrorCode;
import com.passro.passrobackend.global.code.BaseErrorCode;
import com.passro.passrobackend.global.exception.APIException;
import com.passro.passrobackend.global.response.APIResponse;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;


import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class APIExceptionHandler {

    private final View error;

    public APIExceptionHandler(View error) {
        this.error = error;
    }

    @ExceptionHandler(APIException.class)
    public ResponseEntity<APIResponse<Void>> handleAPIException(APIException e) {
        BaseErrorCode code = e.getCode();
        log.warn("APIException 발생: code={}, message={}", code.getCode(), code.getMessage());

        return errorResponse(code, null);
    }

    @ExceptionHandler({MethodArgumentNotValidException.class,
            BindException.class})
    public ResponseEntity<APIResponse<Map<String, String>>> handleValidationException(BindException e) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError fieldError : e.getBindingResult().getFieldErrors()) {
            errors.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
        }

        return errorResponse(CommonErrorCode.INVALID_REQUEST, errors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<APIResponse<Map<String, String>>> handleConstraintViolationException(
            ConstraintViolationException e) {
        log.warn("요청 파라미터 검증 실패: {}", e.getMessage());

        Map<String, String> errors = new LinkedHashMap<>();
        e.getConstraintViolations().forEach(violation -> errors.putIfAbsent(
                violation.getPropertyPath().toString(),
                violation.getMessage()));

        return errorResponse(CommonErrorCode.INVALID_REQUEST, errors);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<APIResponse<Void>> handleDataIntegrityViolation(
            DataIntegrityViolationException e
    ) {
        log.warn("데이터 무결성 제약조건 위반", e);

        return errorResponse(CommonErrorCode.DATA_INTEGRITY_VIOLATION, null);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<APIResponse<Void>> handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException e) {
        log.warn("지원되지 않는 HTTP 메서드 요청: {}", e.getMethod());

        return errorResponse(CommonErrorCode.METHOD_NOT_ALLOWED, null);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<APIResponse<Void>> handleMissingServletRequestParameterException(MissingServletRequestParameterException e) {
        log.warn("요청 파라미터 누락: parameter={}", e.getParameterName());

        return errorResponse(CommonErrorCode.MISSING_REQUEST_PARAMETER, null);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<APIResponse<Void>> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e) {
        log.warn("잘못된 요청 파라미터 타입: parameter={}, requiredType={}", e.getName(), e.getRequiredType());

        return errorResponse(CommonErrorCode.INVALID_REQUEST_PARAMETER_TYPE, null);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<APIResponse<Void>> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        log.warn("요청 페이로드 파싱 실패: {}", e.getMessage());

        return errorResponse(CommonErrorCode.INVALID_REQUEST_BODY, null);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<APIResponse<Void>> handleNoResourceFoundException(NoResourceFoundException e) {
        log.warn("요청한 리소스를 찾을 수 없음: {}", e.getMessage());

        return errorResponse(CommonErrorCode.RESOURCE_NOT_FOUND, null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<APIResponse<Void>> handleException(Exception e) {
        log.error("예상하지 못한 예외 발생", e);

        return errorResponse(CommonErrorCode.INTERNAL_SERVER_ERROR, null);
    }

    private <T> ResponseEntity<APIResponse<T>> errorResponse(BaseErrorCode code, T result) {
        return ResponseEntity
                .status(code.getStatus())
                .body(APIResponse.onFailure(code, result));
    }
}
