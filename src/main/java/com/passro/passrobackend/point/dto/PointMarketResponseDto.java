package com.passro.passrobackend.point.dto;

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
@Schema(types = "object", description = "포인트 내역 마켓 상품 응답")
public class PointMarketResponseDto {

    private Long id;
    private String name;
    private Long price;
    private String category;
    private String imageKey;

    public static PointMarketResponseDto from(Market market) {
        if (market == null) {
            return null;
        }
        return PointMarketResponseDto.builder()
                .id(market.getId())
                .name(market.getName())
                .price(market.getPrice())
                .category(market.categoryOrDefault().getLabel())
                .imageKey(market.getImageKey())
                .build();
    }
}
