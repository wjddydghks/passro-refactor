package com.passro.passrobackend.global.response;

import com.passro.passrobackend.global.code.BaseErrorCode;
import com.passro.passrobackend.global.code.BaseSuccessCode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Nullable;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "API 공통 응답")
public class APIResponse<T> {
    @Schema(description = "요청 성공 여부", example = "true")
    private final boolean isSuccess;

    @Schema(description = "서비스 응답 코드")
    private final String code;

    @Schema(description = "응답 메시지")
    private final String message;

    @Schema(description = "응답 데이터")
    private @Nullable T result;

    public static <T> APIResponse<T> onSuccess(BaseSuccessCode code, T result){
        return new APIResponse<>(true, code.getCode(), code.getMessage(), result);
    }

    public static <T> APIResponse<T> onFailure(BaseErrorCode code, T result){
        return new APIResponse<>(false, code.getCode(), code.getMessage(), result);
    }
}
