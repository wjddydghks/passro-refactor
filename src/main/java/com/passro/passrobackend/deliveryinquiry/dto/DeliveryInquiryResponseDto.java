package com.passro.passrobackend.deliveryinquiry.dto;

import com.passro.passrobackend.deliveryinquiry.entity.DeliveryInquiry;
import com.passro.passrobackend.deliveryinquiry.enums.DeliveryInquiryCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(types = "object", description = "배송 문의 응답 DTO")
public class DeliveryInquiryResponseDto {

    @Schema(description = "문의 ID", example = "1")
    private Long inquiryId;

    @Schema(description = "배송 ID", example = "1")
    private Long deliveryId;

    @Schema(description = "문의 카테고리", example = "DAMAGE")
    private DeliveryInquiryCategory category;

    @Schema(description = "문의 제목", example = "물건이 파손됐어요")
    private String title;

    @Schema(description = "문의 내용", example = "박스가 심하게 찌그러진 채로 도착했습니다.")
    private String content;

    @Schema(description = "첨부 이미지 S3 키 (없으면 null)", example = "inquiry/2026/08/uuid-5678.png")
    private String imageKey;

    @Schema(description = "이미지 다운로드 Presigned URL (imageKey 없으면 null)")
    private String imageUrl;

    @Schema(description = "작성자 닉네임", example = "홍길동")
    private String writerNickname;

    @Schema(description = "작성 시각", example = "2026-08-08T14:00:00")
    private LocalDateTime createdAt;

    public static DeliveryInquiryResponseDto fromDeliveryInquiry(DeliveryInquiry inquiry, String imageUrl) {
        return DeliveryInquiryResponseDto.builder()
                .inquiryId(inquiry.getId())
                .deliveryId(inquiry.getDelivery() != null ? inquiry.getDelivery().getId() : null)
                .category(inquiry.getCategory())
                .title(inquiry.getTitle())
                .content(inquiry.getContent())
                .imageKey(inquiry.getImageKey())
                .imageUrl(imageUrl)
                .writerNickname(inquiry.getAccount() != null ? inquiry.getAccount().getNickname() : null)
                .createdAt(inquiry.getCreatedAt())
                .build();
    }
}
