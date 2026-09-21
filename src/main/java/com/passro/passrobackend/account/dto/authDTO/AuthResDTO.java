package com.passro.passrobackend.account.dto.authDTO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

public class AuthResDTO {

    @Getter
    @AllArgsConstructor
    @Schema(types = "object", description = "로그인 상세 응답")
    public static class TokenResponse {
        private String accessToken;
        private String refreshToken;
    }


}
