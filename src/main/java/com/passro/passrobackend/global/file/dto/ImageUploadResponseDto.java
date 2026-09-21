package com.passro.passrobackend.global.file.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ImageUploadResponseDto {
    private String imageKey;
    private String uploadUrl;
}
