package com.passro.passrobackend.delivery.location.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record ShipperLocationUpdateRequestDto(
        @Schema(description = "현재 위도", example = "37.497942")
        @NotNull(message = "위도는 필수입니다.")
        @DecimalMin(value = "-90.0", message = "위도는 -90 이상이어야 합니다.")
        @DecimalMax(value = "90.0", message = "위도는 90 이하여야 합니다.")
        BigDecimal latitude,

        @Schema(description = "현재 경도", example = "127.027621")
        @NotNull(message = "경도는 필수입니다.")
        @DecimalMin(value = "-180.0", message = "경도는 -180 이상이어야 합니다.")
        @DecimalMax(value = "180.0", message = "경도는 180 이하여야 합니다.")
        BigDecimal longitude,

        @Schema(
                description = "현재 역 Place ID. 생략하면 위도·경도에서 가장 가까운 지하철역을 자동 선택합니다.",
                example = "101",
                nullable = true)
        @Positive(message = "현재 역 ID는 양수여야 합니다.")
        Long placeId
) {
}
