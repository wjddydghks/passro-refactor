package com.passro.passrobackend.inquiry.dto;

import com.passro.passrobackend.inquiry.entity.Inquiry;
import com.passro.passrobackend.inquiry.enums.InquiryCategory;
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
@Schema(types = "object", description = "공통 문의 응답 DTO")
public class InquiryResponseDto {

    @Schema(description = "문의 ID", example = "1")
    private Long inquiryId;

    @Schema(description = "문의 카테고리", example = "ACCOUNT")
    private InquiryCategory category;

    @Schema(description = "문의 제목", example = "로그인이 안 됩니다")
    private String title;

    @Schema(description = "문의 내용", example = "비밀번호 재설정 이메일이 오지 않습니다.")
    private String content;

    @Schema(description = "첨부 이미지 S3 키 (없으면 null)", example = "inquiry/2026/08/uuid-1234.png")
    private String imageKey;

    @Schema(description = "이미지 다운로드 Presigned URL (imageKey 없으면 null)")
    private String imageUrl;

    @Schema(description = "작성자 닉네임", example = "홍길동")
    private String writerNickname;

    @Schema(description = "작성 시각", example = "2026-08-08T14:00:00")
    private LocalDateTime createdAt;

    public static InquiryResponseDto fromInquiry(Inquiry inquiry, String imageUrl) {
        return InquiryResponseDto.builder()
                .inquiryId(inquiry.getId())
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
