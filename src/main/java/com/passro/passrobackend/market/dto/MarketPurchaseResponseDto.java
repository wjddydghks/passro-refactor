package com.passro.passrobackend.market.dto;

import com.passro.passrobackend.market.entity.Market;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Schema(types = "object", description = "마켓 상품 구매 응답")
public class MarketPurchaseResponseDto {

    private MarketItemResponseDto item;

    @Schema(description = "구매 전 포인트", example = "5000")
    private Long beforePoint;

    @Schema(description = "사용 포인트", example = "3000")
    private Long usedPoint;

    @Schema(description = "구매 후 잔여 포인트", example = "2000")
    private Long remainingPoint;

    public static MarketPurchaseResponseDto of(Market market, long beforePoint, long remainingPoint) {
        return MarketPurchaseResponseDto.builder()
                .item(MarketItemResponseDto.from(market))
                .beforePoint(beforePoint)
                .usedPoint(market.getPrice())
                .remainingPoint(remainingPoint)
                .build();
    }
}
