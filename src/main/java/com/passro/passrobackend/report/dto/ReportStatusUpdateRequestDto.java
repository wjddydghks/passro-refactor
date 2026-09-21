package com.passro.passrobackend.report.dto;

import com.passro.passrobackend.report.enums.ReportStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ReportStatusUpdateRequestDto {

    @NotNull(message = "status는 필수입니다.")
    private ReportStatus status;
}
