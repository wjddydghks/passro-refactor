package com.passro.passrobackend.chat.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChatMessageRequestDto(
        @NotBlank(message = "메시지 내용은 비어있을 수 없습니다.")
        @Size(max = 1000, message = "메시지는 1000자 이하여야 합니다.")
        String content,

        @Schema(
                description = "첨부 이미지 S3 키 (선택). POST /file/image/upload-url로 발급받은 값",
                example = "uploads/images/123e4567-e89b-12d3-a456-426614174000.png",
                nullable = true)
        @Size(max = 500, message = "이미지 키는 500자 이하여야 합니다.")
        String imageKey
) {
    public ChatMessageRequestDto(String content) {
        this(content, null);
    }
}
