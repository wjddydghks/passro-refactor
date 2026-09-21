package com.passro.passrobackend.report.controller;

import com.passro.passrobackend.account.entity.Account;
import com.passro.passrobackend.global.response.APIResponse;
import com.passro.passrobackend.report.code.ReportSuccessCode;
import com.passro.passrobackend.report.dto.MyReportListResponseDto;
import com.passro.passrobackend.report.dto.ReportCreateRequestDto;
import com.passro.passrobackend.report.dto.ReportCreateResponseDto;
import com.passro.passrobackend.report.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Report", description = "신고 API")
@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "신고 작성", description = "배송, 채팅 메시지, 사용자를 신고합니다.")
    public APIResponse<ReportCreateResponseDto> createReport(
            @Parameter(hidden = true) @AuthenticationPrincipal(expression = "account") Account account,
            @Valid @RequestBody ReportCreateRequestDto request
    ) {
        return APIResponse.onSuccess(
                ReportSuccessCode.REPORT_CREATED,
                reportService.createReport(account, request)
        );
    }

    @GetMapping("/me")
    @Operation(summary = "내 신고 목록 조회", description = "내가 작성한 신고 내역과 처리 상태를 최신순으로 조회합니다.")
    public APIResponse<MyReportListResponseDto> getMyReports(
            @Parameter(hidden = true) @AuthenticationPrincipal(expression = "account") Account account,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return APIResponse.onSuccess(
                ReportSuccessCode.MY_REPORTS_FOUND,
                reportService.getMyReports(account, pageable)
        );
    }
}
