package com.passro.passrobackend.delivery.location.dto;

import com.passro.passrobackend.delivery.location.model.ShipperLocation;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(
        name = "ShipperLocationResponse",
        types = "object",
        description = "배송기사 현재 위치와 배송 도착 예상시간")
public record ShipperLocationResponseDto(
        @Schema(description = "배송기사 현재 위도", example = "37.497942")
        BigDecimal latitude,

        @Schema(description = "배송기사 현재 경도", example = "127.027621")
        BigDecimal longitude,

        @Schema(description = "현재 위치에서 가장 가까운 지하철역 Place ID", example = "101")
        Long placeId,

        @Schema(description = "위치가 마지막으로 갱신된 시각", example = "2026-08-07T10:30:00")
        LocalDateTime updatedAt,

        @Schema(
                description = "현재 역부터 배송 도착역까지의 예상 소요시간(분). 위치 갱신 응답에서는 제공되지 않습니다.",
                example = "15",
                nullable = true)
        Integer estimatedTimeMinutes
) {
    public static ShipperLocationResponseDto from(ShipperLocation location) {
        return from(location, null);
    }

    public static ShipperLocationResponseDto from(
            ShipperLocation location,
            Integer estimatedTimeMinutes) {
        return new ShipperLocationResponseDto(
                location.latitude(),
                location.longitude(),
                location.placeId(),
                location.updatedAt(),
                estimatedTimeMinutes);
    }
}
