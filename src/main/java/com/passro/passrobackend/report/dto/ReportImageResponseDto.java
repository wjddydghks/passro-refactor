package com.passro.passrobackend.report.dto;

import com.passro.passrobackend.report.entity.ReportImage;
import java.util.function.Function;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ReportImageResponseDto {

    private String imageKey;
    private Integer displayOrder;

    public static ReportImageResponseDto from(
            ReportImage reportImage, Function<String, String> imageUrlResolver) {
        return new ReportImageResponseDto(
                imageUrlResolver.apply(reportImage.getImageKey()),
                reportImage.getDisplayOrder()
        );
    }
}
