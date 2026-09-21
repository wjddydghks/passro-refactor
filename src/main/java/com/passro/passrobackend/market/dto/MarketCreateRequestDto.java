package com.passro.passrobackend.market.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Schema(types = "object", description = "마켓 상품 등록 요청")
public class MarketCreateRequestDto {

    @NotBlank(message = "상품명을 입력하세요.")
    @Size(max = 255, message = "상품명은 255자 이하여야 합니다.")
    @Schema(description = "상품명", example = "스타벅스 아메리카노")
    private String name;

    @NotNull(message = "상품 가격을 입력하세요.")
    @Positive(message = "상품 가격은 0보다 커야 합니다.")
    @Schema(description = "상품 가격(포인트)", example = "4000")
    private Long price;

    @NotBlank(message = "상품 카테고리를 입력하세요.")
    @Schema(description = "상품 카테고리", example = "카페", allowableValues = {"음식", "카페", "편의점", "기타"})
    private String category;

    @NotBlank(message = "상품 이미지 키를 입력하세요.")
    @Size(max = 512, message = "이미지 키는 512자 이하여야 합니다.")
    @Schema(
            description = "POST /file/image/upload-url에서 발급받은 임시 이미지 키",
            example = "uploads/images/123e4567-e89b-12d3-a456-426614174000.png")
    private String imageKey;
}
