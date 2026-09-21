package com.passro.passrobackend.report.controller;

import com.passro.passrobackend.global.response.APIResponse;
import com.passro.passrobackend.report.code.ReportSuccessCode;
import com.passro.passrobackend.report.dto.ReportStatusUpdateRequestDto;
import com.passro.passrobackend.report.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Admin Report", description = "관리자 신고 처리 API")
@RestController
@RequestMapping("/admin/reports")
@RequiredArgsConstructor
public class AdminReportController {

    private final ReportService reportService;

    @PatchMapping("/{reportId}/status")
    @Operation(summary = "신고 상태 변경", description = "관리자가 신고 처리 상태를 변경합니다.")
    public APIResponse<Void> updateReportStatus(
            @PathVariable Long reportId,
            @Valid @RequestBody ReportStatusUpdateRequestDto request
    ) {
        reportService.updateReportStatus(reportId, request);
        return APIResponse.onSuccess(ReportSuccessCode.REPORT_STATUS_UPDATED, null);
    }
}
