package com.passro.passrobackend.domain.report.service;

import com.passro.passrobackend.domain.account.entity.Account;
import com.passro.passrobackend.domain.report.dto.MyReportListResponseDto;
import com.passro.passrobackend.domain.report.dto.ReportCreateRequestDto;
import com.passro.passrobackend.domain.report.dto.ReportCreateResponseDto;
import com.passro.passrobackend.domain.report.dto.ReportStatusUpdateRequestDto;
import org.springframework.data.domain.Pageable;

public interface ReportService {

    ReportCreateResponseDto createReport(Account reporter, ReportCreateRequestDto request);

    MyReportListResponseDto getMyReports(Account reporter, Pageable pageable);

    void updateReportStatus(Long reportId, ReportStatusUpdateRequestDto request);
}
