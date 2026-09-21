package com.passro.passrobackend.subway.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SubwayRouteRequestDto {

    @Schema(description = "출발역 Place ID", example = "101", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "출발역 Place ID는 필수입니다.")
    private Long originPlaceId;

    @Schema(description = "입력 순서대로 방문할 경유역 Place ID 목록", example = "[205, 310]")
    private List<@NotNull(message = "경유역 Place ID는 null일 수 없습니다.") Long> waypointPlaceIds;

    @Schema(description = "도착역 Place ID", example = "420", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "도착역 Place ID는 필수입니다.")
    private Long destinationPlaceId;
}
