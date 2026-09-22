package com.passro.passrobackend.domain.report.dto;

import com.passro.passrobackend.domain.report.entity.Report;
import com.passro.passrobackend.domain.report.enums.ReportStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ReportCreateResponseDto {

    private Long reportId;
    private ReportStatus status;

    public static ReportCreateResponseDto from(Report report) {
        return new ReportCreateResponseDto(
                report.getId(),
                report.getStatus()
        );
    }
}
