package com.passro.passrobackend.sender.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SenderDeliveryCreateRequestDto {
    @NotNull(message = "출발역 Place ID를 입력하세요.")
    @Positive(message = "출발역 Place ID는 양수여야 합니다.")
    private Long sourceStationId;

    @NotNull(message = "도착역 Place ID를 입력하세요.")
    @Positive(message = "도착역 Place ID는 양수여야 합니다.")
    private Long destinationStationId;

    // Delivery
    @NotBlank(message = "물품명을 입력하세요.")
    private String name;

    // DeliveryGoodInfo
    @NotBlank(message = "물품 크기를 입력하세요.")
    @Pattern(regexp = "(?i)S|M|L", message = "물품 크기는 S, M, L 중 하나여야 합니다.")
    private String size;
    private String picture;

    // Delivery memo
    private String memo;

}
