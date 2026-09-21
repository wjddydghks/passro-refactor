package com.passro.passrobackend.point.dto;

import com.passro.passrobackend.point.entity.PointLog;
import com.passro.passrobackend.point.enums.PointIncrementReason;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Schema(types = "object", description = "포인트 증감 내역 응답")
public class PointLogResponseDto {

    private Long pointLogId;
    private PointDeliveryResponseDto delivery;
    private PointMarketResponseDto market;
    private PointIncrementReason incrementReason;
    private Long deltaPoint;
    private Long beforePoint;
    private Long afterPoint;
    private String incrementReasonMemo;
    private LocalDateTime createdAt;

    public static PointLogResponseDto from(PointLog pointLog) {
        return PointLogResponseDto.builder()
                .pointLogId(pointLog.getId())
                .delivery(PointDeliveryResponseDto.from(pointLog.getDelivery()))
                .market(PointMarketResponseDto.from(pointLog.getMarket()))
                .incrementReason(pointLog.getIncrementReason())
                .deltaPoint(pointLog.getDeltaPoint())
                .beforePoint(pointLog.getBeforePoint())
                .afterPoint(pointLog.getAfterPoint())
                .incrementReasonMemo(pointLog.getIncrementReasonMemo())
                .createdAt(pointLog.getCreatedAt())
                .build();
    }
}
