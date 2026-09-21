package com.passro.passrobackend.subway.controller;

import com.passro.passrobackend.global.response.APIResponse;
import com.passro.passrobackend.place.service.PlaceService;
import com.passro.passrobackend.subway.code.SubwaySuccessCode;
import com.passro.passrobackend.subway.dto.SubwayStationResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static com.passro.passrobackend.global.configuration.SwaggerErrorExamples.COMMON_VALIDATION;
import static com.passro.passrobackend.global.configuration.SwaggerSuccessExamples.SUBWAY_STATION_LIST;

@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping("/subway/search")
@SecurityRequirements
@Tag(name = "지하철", description = "지하철역 검색 및 최단 경로 탐색 API")
public class SubwayStationController {

    private final PlaceService placeService;

    @GetMapping
    @Operation(
            summary = "지하철역 검색",
            description = "노선명 또는 역명에 검색어가 포함된 지하철역을 조회합니다. 검색어는 한글과 숫자만 입력할 수 있습니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "지하철역 검색 성공", useReturnTypeSchema = true,
                    content = @Content(examples = @ExampleObject(
                            name = "SUBWAY200_2", summary = "지하철역 검색 성공", value = SUBWAY_STATION_LIST))),
            @ApiResponse(responseCode = "400", description = "검색어 검증 실패",
                    content = @Content(schema = @Schema(implementation = APIResponse.class),
                            examples = @ExampleObject(name = "COMMON400", value = COMMON_VALIDATION)))
    })
    public APIResponse<List<SubwayStationResponseDto>> search(
            @Parameter(description = "노선명 또는 역명 검색어", example = "강남", required = true)
            @RequestParam
            @Pattern(regexp = "^[가-힣ㄱ-ㅎㅏ-ㅣ0-9]+$", message = "검색어는 한글과 숫자만 입력 가능합니다.")
            String keyword) {
        List<SubwayStationResponseDto> stations = placeService.searchByKeyword(keyword).stream()
                .map(SubwayStationResponseDto::from)
                .toList();
        return APIResponse.onSuccess(SubwaySuccessCode.STATION_SEARCH_OK, stations);
    }
}
