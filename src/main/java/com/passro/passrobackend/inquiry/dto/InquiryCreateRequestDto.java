package com.passro.passrobackend.inquiry.dto;

import com.passro.passrobackend.inquiry.enums.InquiryCategory;
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
@Schema(types = "object", description = "공통 문의 작성 요청 DTO")
public class InquiryCreateRequestDto {

    @Schema(description = "문의 카테고리 (ACCOUNT, PAYMENT, DELIVERY, SERVICE, BUG, ETC)", example = "ACCOUNT")
    @NotNull(message = "카테고리는 필수입니다.")
    private InquiryCategory category;

    @Schema(description = "문의 제목", example = "로그인이 안 됩니다")
    @NotBlank(message = "제목은 필수입니다.")
    @Size(max = 255, message = "제목은 255자 이하여야 합니다.")
    private String title;

    @Schema(description = "문의 내용", example = "비밀번호 재설정 이메일이 오지 않습니다.")
    @NotBlank(message = "내용은 필수입니다.")
    private String content;

    @Schema(description = "첨부 이미지 S3 키 (선택). POST /file/image/upload-url 로 발급받은 값", example = "inquiry/2026/08/uuid-1234.png")
    @Size(max = 512, message = "이미지 키는 512자 이하여야 합니다.")
    private String imageKey;
}
