package com.passro.passrobackend.report.service;

import com.passro.passrobackend.account.entity.Account;
import com.passro.passrobackend.account.repository.AccountRepository;
import com.passro.passrobackend.chat.entity.ChatMessage;
import com.passro.passrobackend.chat.repository.ChatMessageRepository;
import com.passro.passrobackend.delivery.entity.Delivery;
import com.passro.passrobackend.delivery.repository.DeliveryRepository;
import com.passro.passrobackend.file.service.S3Service;
import com.passro.passrobackend.report.dto.MyReportItemResponseDto;
import com.passro.passrobackend.report.dto.MyReportListResponseDto;
import com.passro.passrobackend.report.dto.ReportCreateRequestDto;
import com.passro.passrobackend.report.dto.ReportCreateResponseDto;
import com.passro.passrobackend.report.dto.ReportStatusUpdateRequestDto;
import com.passro.passrobackend.report.entity.Report;
import com.passro.passrobackend.report.entity.ReportImage;
import com.passro.passrobackend.report.enums.ReportReason;
import com.passro.passrobackend.report.enums.ReportStatus;
import com.passro.passrobackend.report.exception.ReportException;
import com.passro.passrobackend.report.exception.code.ReportErrorCode;
import com.passro.passrobackend.report.repository.ReportImageRepository;
import com.passro.passrobackend.report.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportServiceImpl implements ReportService {

    private static final String REPORT_IMAGE_DIRECTORY = "report-images/";
    private static final int MAX_REPORT_IMAGE_COUNT = 5;

    private final ReportRepository reportRepository;
    private final ReportImageRepository reportImageRepository;
    private final DeliveryRepository deliveryRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final AccountRepository accountRepository;
    private final S3Service s3Service;

    @Override
    @Transactional
    public ReportCreateResponseDto createReport(Account reporter, ReportCreateRequestDto request) {
        validateCommon(request);

        ResolvedTarget resolvedTarget = resolveTarget(reporter, request);

        if (reportRepository.existsByReporter_IdAndTargetTypeAndTargetId(
                reporter.getId(),
                request.getTargetType(),
                resolvedTarget.targetId()
        )) {
            throw new ReportException(ReportErrorCode.REPORT_ALREADY_EXISTS);
        }

        Report report = Report.builder()
                .reporter(reporter)
                .reportedAccount(resolvedTarget.reportedAccount())
                .delivery(resolvedTarget.delivery())
                .chatMessage(resolvedTarget.chatMessage())
                .targetType(request.getTargetType())
                .targetId(resolvedTarget.targetId())
                .reason(request.getReason())
                .detail(request.getDetail())
                .status(ReportStatus.PENDING)
                .build();

        try {
            reportRepository.saveAndFlush(report);
        } catch (DataIntegrityViolationException e) {
            throw new ReportException(ReportErrorCode.REPORT_ALREADY_EXISTS);
        }

        saveImages(report, request.getImageKeys());

        return ReportCreateResponseDto.from(report);
    }

    @Override
    public MyReportListResponseDto getMyReports(Account reporter, Pageable pageable) {
        Page<Report> page = reportRepository.findAllByReporter_IdOrderByCreatedAtDesc(
                reporter.getId(),
                pageable
        );

        return new MyReportListResponseDto(
                page.getContent().stream()
                        .map(report -> MyReportItemResponseDto.from(
                                report, this::imageUrl))
                        .toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.hasNext()
        );
    }

    private String imageUrl(String imageKey) {
        if (imageKey == null || imageKey.isBlank()) {
            return null;
        }
        return s3Service.getPresignedDownloadUrlString(imageKey);
    }

    @Override
    @Transactional
    public void updateReportStatus(Long reportId, ReportStatusUpdateRequestDto request) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ReportException(ReportErrorCode.REPORT_NOT_FOUND));

        report.updateStatus(request.getStatus());
    }

    private void validateCommon(ReportCreateRequestDto request) {
        if (request.getTargetType() == null) {
            throw new ReportException(ReportErrorCode.INVALID_REPORT_TARGET_TYPE);
        }

        if (request.getReason() == null) {
            throw new ReportException(ReportErrorCode.INVALID_REPORT_REASON);
        }

        if (request.getImageKeys() != null && request.getImageKeys().size() > MAX_REPORT_IMAGE_COUNT) {
            throw new ReportException(ReportErrorCode.INVALID_REPORT_IMAGE_COUNT);
        }

        if (request.getReason() == ReportReason.OTHER && !StringUtils.hasText(request.getDetail())) {
            throw new ReportException(ReportErrorCode.INVALID_REPORT_OTHER_DETAIL);
        }
    }

    private ResolvedTarget resolveTarget(Account reporter, ReportCreateRequestDto request) {
        return switch (request.getTargetType()) {
            case DELIVERY -> resolveDeliveryTarget(reporter, request);
            case CHAT_MESSAGE -> resolveChatMessageTarget(reporter, request);
            case ACCOUNT -> resolveAccountTarget(reporter, request);
        };
    }

    private ResolvedTarget resolveDeliveryTarget(Account reporter, ReportCreateRequestDto request) {
        if (request.getDeliveryId() == null) {
            throw new ReportException(ReportErrorCode.INVALID_REPORT_DELIVERY_ID);
        }

        Delivery delivery = deliveryRepository.findById(request.getDeliveryId())
                .orElseThrow(() -> new ReportException(ReportErrorCode.REPORT_DELIVERY_NOT_FOUND));

        if (!isParticipant(reporter, delivery)) {
            throw new ReportException(ReportErrorCode.REPORT_FORBIDDEN);
        }

        Account reportedAccount = resolveCounterparty(reporter, delivery);

        return new ResolvedTarget(
                delivery.getId(),
                reportedAccount,
                delivery,
                null
        );
    }

    private ResolvedTarget resolveChatMessageTarget(Account reporter, ReportCreateRequestDto request) {
        if (request.getChatMessageId() == null) {
            throw new ReportException(ReportErrorCode.INVALID_REPORT_CHAT_MESSAGE_ID);
        }

        ChatMessage chatMessage = chatMessageRepository.findById(request.getChatMessageId())
                .orElseThrow(() -> new ReportException(ReportErrorCode.REPORT_CHAT_MESSAGE_NOT_FOUND));

        Delivery delivery = chatMessage.getDelivery();
        if (!isParticipant(reporter, delivery)) {
            throw new ReportException(ReportErrorCode.REPORT_FORBIDDEN);
        }

        if (chatMessage.getSender().getId().equals(reporter.getId())) {
            throw new ReportException(ReportErrorCode.REPORT_SELF_MESSAGE_NOT_ALLOWED);
        }

        return new ResolvedTarget(
                chatMessage.getId(),
                chatMessage.getSender(),
                delivery,
                chatMessage
        );
    }

    private ResolvedTarget resolveAccountTarget(Account reporter, ReportCreateRequestDto request) {
        if (request.getReportedAccountId() == null) {
            throw new ReportException(ReportErrorCode.INVALID_REPORTED_ACCOUNT_ID);
        }
        if (request.getDeliveryId() == null) {
            throw new ReportException(ReportErrorCode.INVALID_REPORT_DELIVERY_ID);
        }

        Account reportedAccount = accountRepository.findById(request.getReportedAccountId())
                .orElseThrow(() -> new ReportException(ReportErrorCode.REPORT_ACCOUNT_NOT_FOUND));

        if (reportedAccount.getId().equals(reporter.getId())) {
            throw new ReportException(ReportErrorCode.REPORT_SELF_ACCOUNT_NOT_ALLOWED);
        }

        Delivery delivery = deliveryRepository.findById(request.getDeliveryId())
                .orElseThrow(() -> new ReportException(ReportErrorCode.REPORT_DELIVERY_NOT_FOUND));

        if (!isParticipant(reporter, delivery) || !isParticipant(reportedAccount, delivery)) {
            throw new ReportException(ReportErrorCode.REPORT_FORBIDDEN);
        }

        if (resolveCounterparty(reporter, delivery) == null
                || !resolveCounterparty(reporter, delivery).getId().equals(reportedAccount.getId())) {
            throw new ReportException(ReportErrorCode.REPORT_FORBIDDEN);
        }

        return new ResolvedTarget(
                reportedAccount.getId(),
                reportedAccount,
                delivery,
                null
        );
    }

    private boolean isParticipant(Account account, Delivery delivery) {
        return delivery.getSender().getId().equals(account.getId())
                || (delivery.getShipper() != null && delivery.getShipper().getId().equals(account.getId()));
    }

    private Account resolveCounterparty(Account reporter, Delivery delivery) {
        if (delivery.getSender().getId().equals(reporter.getId())) {
            return delivery.getShipper();
        }

        if (delivery.getShipper() != null && delivery.getShipper().getId().equals(reporter.getId())) {
            return delivery.getSender();
        }

        return null;
    }

    private void saveImages(Report report, List<String> imageKeys) {
        List<String> keys = imageKeys == null ? Collections.emptyList() : imageKeys;

        for (int i = 0; i < keys.size(); i++) {
            String finalizedKey = s3Service.finalizeUploadedImage(keys.get(i), REPORT_IMAGE_DIRECTORY);

            ReportImage reportImage = ReportImage.builder()
                    .imageKey(finalizedKey)
                    .displayOrder(i)
                    .build();

            report.addImage(reportImage);
            reportImageRepository.save(reportImage);
        }
    }

    private record ResolvedTarget(
            Long targetId,
            Account reportedAccount,
            Delivery delivery,
            ChatMessage chatMessage
    ) {
    }
}
