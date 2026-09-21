package com.passro.passrobackend.subway.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.passro.passrobackend.place.entity.Place;
import com.passro.passrobackend.subway.graph.SubwayNode;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "최단 경로에 포함된 지하철역", types = "object")
public class SubwayStationResponseDto {

    @Schema(description = "Place ID", example = "101")
    private Long id;

    @Schema(description = "권역명. 최단 경로 응답에서 제공됩니다.", example = "수도권", nullable = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String region;

    @Schema(description = "지하철 노선명", example = "2호선")
    private String routeName;

    @Schema(description = "지하철역명", example = "강남")
    private String stationName;

    @Schema(description = "역 위도", example = "37.497942", nullable = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private BigDecimal latitude;

    @Schema(description = "역 경도", example = "127.027621", nullable = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private BigDecimal longitude;

    public SubwayStationResponseDto(Long id, String region, String routeName, String stationName) {
        this(id, region, routeName, stationName, null, null);
    }

    public static SubwayStationResponseDto from(SubwayNode node) {
        return new SubwayStationResponseDto(
                node.getId(),
                node.getRegion(),
                node.getRoute(),
                node.getName(),
                node.getLatitude(),
                node.getLongitude());
    }

    public static SubwayStationResponseDto from(Place place) {
        return new SubwayStationResponseDto(
                place.getId(),
                null,
                place.getSubwayRouteName(),
                place.getSubwayStationName(),
                place.getLatitude(),
                place.getLongitude());
    }
}
