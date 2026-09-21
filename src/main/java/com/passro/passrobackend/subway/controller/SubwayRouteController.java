package com.passro.passrobackend.subway.controller;

import com.passro.passrobackend.global.exception.APIException;
import com.passro.passrobackend.global.response.APIResponse;
import com.passro.passrobackend.subway.code.SubwayErrorCode;
import com.passro.passrobackend.subway.code.SubwaySuccessCode;
import com.passro.passrobackend.subway.dto.SubwayRouteRequestDto;
import com.passro.passrobackend.subway.dto.SubwayRouteResponseDto;
import com.passro.passrobackend.subway.service.SubwayService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.passro.passrobackend.global.configuration.SwaggerErrorExamples.COMMON_VALIDATION;
import static com.passro.passrobackend.global.configuration.SwaggerErrorExamples.SUBWAY_PLACE_NOT_FOUND;
import static com.passro.passrobackend.global.configuration.SwaggerErrorExamples.SUBWAY_ROUTE_NOT_FOUND;
import static com.passro.passrobackend.global.configuration.SwaggerSuccessExamples.SUBWAY_SHORTEST_ROUTE;

@RestController
@RequestMapping("/subway/routes")
@RequiredArgsConstructor
@SecurityRequirements
@Tag(name = "지하철", description = "지하철역 검색 및 최단 경로 탐색 API")
public class SubwayRouteController {

    private final SubwayService subwayService;

    @PostMapping("/shortest")
    @Operation(
            summary = "최단 경로 탐색",
            description = "Place ID로 출발역과 도착역을 지정하고, 선택적으로 경유역을 입력 순서대로 지나가는 최단 경로를 탐색합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "최단 경로 탐색 성공", useReturnTypeSchema = true,
                    content = @Content(examples = @ExampleObject(
                            name = "SUBWAY200_1", summary = "최단 경로 탐색 성공", value = SUBWAY_SHORTEST_ROUTE))),
            @ApiResponse(responseCode = "400", description = "요청 값 검증 실패",
                    content = @Content(schema = @Schema(implementation = APIResponse.class),
                            examples = @ExampleObject(name = "COMMON400", value = COMMON_VALIDATION))),
            @ApiResponse(responseCode = "404", description = "지하철역 또는 경로를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = APIResponse.class), examples = {
                            @ExampleObject(name = "SUBWAY404_1", summary = "지하철역 없음", value = SUBWAY_PLACE_NOT_FOUND),
                            @ExampleObject(name = "SUBWAY404_2", summary = "경로 없음", value = SUBWAY_ROUTE_NOT_FOUND)
                    }))
    })
    public APIResponse<SubwayRouteResponseDto> findShortestRoute(
            @Valid @RequestBody SubwayRouteRequestDto request) {
        try {
            SubwayRouteResponseDto route = subwayService.findShortestRouteByPlaceIds(
                    request.getOriginPlaceId(),
                    request.getWaypointPlaceIds(),
                    request.getDestinationPlaceId());
            return APIResponse.onSuccess(SubwaySuccessCode.ROUTE_OK, route);
        } catch (IllegalArgumentException exception) {
            throw new APIException(SubwayErrorCode.PLACE_NOT_FOUND);
        } catch (IllegalStateException exception) {
            throw new APIException(SubwayErrorCode.ROUTE_NOT_FOUND);
        }
    }
}
