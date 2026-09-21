package com.passro.passrobackend.report.dto;

import com.passro.passrobackend.report.enums.ReportReason;
import com.passro.passrobackend.report.enums.ReportTargetType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ReportCreateRequestDto {

    @NotNull(message = "targetType은 필수입니다.")
    private ReportTargetType targetType;

    @NotNull(message = "reason은 필수입니다.")
    private ReportReason reason;

    @Size(max = 1000, message = "detail은 최대 1000자까지 입력할 수 있습니다.")
    private String detail;

    @Size(max = 5, message = "이미지는 최대 5장까지 첨부할 수 있습니다.")
    private List<String> imageKeys;

    private Long deliveryId;

    private Long chatMessageId;

    private Long reportedAccountId;
}
