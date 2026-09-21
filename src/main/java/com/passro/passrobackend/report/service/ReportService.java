package com.passro.passrobackend.report.service;

import com.passro.passrobackend.account.entity.Account;
import com.passro.passrobackend.report.dto.MyReportListResponseDto;
import com.passro.passrobackend.report.dto.ReportCreateRequestDto;
import com.passro.passrobackend.report.dto.ReportCreateResponseDto;
import com.passro.passrobackend.report.dto.ReportStatusUpdateRequestDto;
import org.springframework.data.domain.Pageable;

public interface ReportService {

    ReportCreateResponseDto createReport(Account reporter, ReportCreateRequestDto request);

    MyReportListResponseDto getMyReports(Account reporter, Pageable pageable);

    void updateReportStatus(Long reportId, ReportStatusUpdateRequestDto request);
}
