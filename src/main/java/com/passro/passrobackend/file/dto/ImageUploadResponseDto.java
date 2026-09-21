package com.passro.passrobackend.file.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ImageUploadResponseDto {
    private String imageKey;
    private String uploadUrl;
}
