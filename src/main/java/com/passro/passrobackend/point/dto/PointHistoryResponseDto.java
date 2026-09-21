package com.passro.passrobackend.point.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Schema(types = "object", description = "포인트 내역 응답")
public class PointHistoryResponseDto {

    private Long currentPoint;
    private List<PointLogResponseDto> pointLogs;
}
