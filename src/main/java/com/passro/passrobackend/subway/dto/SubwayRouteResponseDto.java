package com.passro.passrobackend.subway.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "지하철 경로 조회 응답 DTO", types = "object")
public class SubwayRouteResponseDto {

    @Schema(description = "최단 경로의 역간 이동 횟수", example = "5")
    private int shortestDistance;

    @Schema(description = "환승 횟수", example = "1")
    private int transferCount;

    @Schema(description = "출발역부터 도착역까지 순서대로 정렬된 역 목록")
    private List<SubwayStationResponseDto> stations;

    /**
     * 환승 횟수가 차감된 순수 이동 정거장 수 계산
     */
    public int getTravelStationCount() {
        int graphEdges = Math.max(0, stations == null ? 0 : stations.size() - 1);
        return Math.max(0, graphEdges - transferCount);
    }
}
