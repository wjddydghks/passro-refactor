package com.passro.passrobackend.report.dto;

import com.passro.passrobackend.report.entity.Report;
import com.passro.passrobackend.report.enums.ReportReason;
import com.passro.passrobackend.report.enums.ReportStatus;
import com.passro.passrobackend.report.enums.ReportTargetType;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

@Getter
@AllArgsConstructor
public class MyReportItemResponseDto {

    private Long reportId;
    private ReportTargetType targetType;
    private Long targetId;
    private Long deliveryId;
    private Long chatMessageId;
    private Long reportedAccountId;
    private ReportReason reason;
    private String detail;
    private ReportStatus status;
    private List<ReportImageResponseDto> images;
    private LocalDateTime createdAt;

    public static MyReportItemResponseDto from(
            Report report, Function<String, String> imageUrlResolver) {
        return new MyReportItemResponseDto(
                report.getId(),
                report.getTargetType(),
                report.getTargetId(),
                report.getDelivery() != null ? report.getDelivery().getId() : null,
                report.getChatMessage() != null ? report.getChatMessage().getId() : null,
                report.getReportedAccount() != null ? report.getReportedAccount().getId() : null,
                report.getReason(),
                report.getDetail(),
                report.getStatus(),
                report.getImages().stream()
                        .sorted(Comparator.comparing(ri -> ri.getDisplayOrder()))
                        .map(image -> ReportImageResponseDto.from(image, imageUrlResolver))
                        .toList(),
                report.getCreatedAt()
        );
    }
}
