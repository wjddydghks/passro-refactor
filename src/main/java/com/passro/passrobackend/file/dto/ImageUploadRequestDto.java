package com.passro.passrobackend.file.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ImageUploadRequestDto {
    @NotBlank
    private String fileName;

    @NotBlank
    private String contentType;

    @Min(1)
    @Max(10 * 1024 * 1024)
    private long fileSize;
}
