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
@Schema(types = "object", description = "마켓 상품 응답")
public class MarketItemResponseDto {

    @Schema(description = "상품 ID", example = "1")
    private Long id;

    @Schema(description = "상품명", example = "커피 쿠폰")
    private String name;

    @Schema(description = "구매 가격(포인트)", example = "3000")
    private Long price;

    @Schema(description = "상품 카테고리", example = "카페", allowableValues = {"음식", "카페", "편의점", "기타"})
    private String category;

    @Schema(
            description = "상품 이미지 S3 객체 키",
            example = "uploads/images/123e4567-e89b-12d3-a456-426614174000.png",
            nullable = true)
    private String imageKey;

    public static MarketItemResponseDto from(Market market) {
        return MarketItemResponseDto.builder()
                .id(market.getId())
                .name(market.getName())
                .price(market.getPrice())
                .category(market.categoryOrDefault().getLabel())
                .imageKey(market.getImageKey())
                .build();
    }
}
