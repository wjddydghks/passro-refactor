package com.passro.passrobackend.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import io.swagger.v3.oas.annotations.media.Schema;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(types = "object", description = "미확인 알림 수 응답 DTO")

public class UnreadCountResponseDto {

    @Schema(description = "미확인 알림 수", example = "3")
    private long unreadCount;

}
