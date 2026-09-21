package com.passro.passrobackend.deliveryinquiry.dto;

import com.passro.passrobackend.deliveryinquiry.enums.DeliveryInquiryCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
@Schema(types = "object", description = "배송 문의 작성 요청 DTO")
public class DeliveryInquiryCreateRequestDto {

    @Schema(description = "문의 대상 배송 ID", example = "1")
    @NotNull(message = "배송 ID는 필수입니다.")
    private Long deliveryId;

    @Schema(description = "문의 카테고리 (DELAY, DAMAGE, LOST, WRONG_DELIVERY, POINT, ETC)", example = "DAMAGE")
    @NotNull(message = "카테고리는 필수입니다.")
    private DeliveryInquiryCategory category;

    // 제목은 선택 입력 (ERD상 nullable)
    @Schema(description = "문의 제목 (선택)", example = "물건이 파손됐어요")
    @Size(max = 255, message = "제목은 255자 이하여야 합니다.")
    private String title;

    @Schema(description = "문의 내용", example = "박스가 심하게 찌그러진 채로 도착했습니다.")
    @NotBlank(message = "내용은 필수입니다.")
    private String content;

    @Schema(description = "첨부 이미지 S3 키 (선택). POST /file/image/upload-url 로 발급받은 값", example = "inquiry/2026/08/uuid-5678.png")
    @Size(max = 512, message = "이미지 키는 512자 이하여야 합니다.")
    private String imageKey;
}
