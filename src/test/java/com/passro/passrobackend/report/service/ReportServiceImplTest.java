package com.passro.passrobackend.report.service;

import com.passro.passrobackend.account.entity.Account;
import com.passro.passrobackend.account.repository.AccountRepository;
import com.passro.passrobackend.chat.entity.ChatMessage;
import com.passro.passrobackend.chat.repository.ChatMessageRepository;
import com.passro.passrobackend.delivery.entity.Delivery;
import com.passro.passrobackend.delivery.repository.DeliveryRepository;
import com.passro.passrobackend.file.service.S3Service;
import com.passro.passrobackend.report.dto.MyReportListResponseDto;
import com.passro.passrobackend.report.dto.ReportCreateRequestDto;
import com.passro.passrobackend.report.dto.ReportStatusUpdateRequestDto;
import com.passro.passrobackend.report.entity.Report;
import com.passro.passrobackend.report.entity.ReportImage;
import com.passro.passrobackend.report.enums.ReportReason;
import com.passro.passrobackend.report.enums.ReportStatus;
import com.passro.passrobackend.report.enums.ReportTargetType;
import com.passro.passrobackend.report.exception.ReportException;
import com.passro.passrobackend.report.exception.code.ReportErrorCode;
import com.passro.passrobackend.report.repository.ReportImageRepository;
import com.passro.passrobackend.report.repository.ReportRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReportServiceImplTest {

    @Mock
    private ReportRepository reportRepository;
    @Mock
    private ReportImageRepository reportImageRepository;
    @Mock
    private DeliveryRepository deliveryRepository;
    @Mock
    private ChatMessageRepository chatMessageRepository;
    @Mock
    private AccountRepository accountRepository;
    @Mock
    private S3Service s3Service;

    @InjectMocks
    private ReportServiceImpl reportService;

    @Test
    void createDeliveryReport_success() {
        // given
        Account reporter = mockAccount(1L);
        Account counterparty = mockAccount(2L);
        Delivery delivery = mockDelivery(10L, reporter, counterparty);

        ReportCreateRequestDto request = new ReportCreateRequestDto(
                ReportTargetType.DELIVERY,
                ReportReason.FRAUD,
                "배송 관련 신고",
                null,
                10L,
                null,
                null
        );

        given(deliveryRepository.findById(10L)).willReturn(Optional.of(delivery));
        given(reportRepository.existsByReporter_IdAndTargetTypeAndTargetId(1L, ReportTargetType.DELIVERY, 10L))
                .willReturn(false);
        given(reportRepository.saveAndFlush(any(Report.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        // when
        var response = reportService.createReport(reporter, request);

        // then
        assertThat(response).isNotNull();
        verify(reportRepository).saveAndFlush(any(Report.class));
        verify(reportImageRepository, never()).save(any(ReportImage.class));
        verify(s3Service, never()).finalizeUploadedImage(anyString(), anyString());
    }

    @Test
    void createChatMessageReport_success() {
        // given
        Account reporter = mockAccount(1L);
        Account counterparty = mockAccount(2L);
        Delivery delivery = mockDelivery(10L, reporter, counterparty);
        ChatMessage chatMessage = mockChatMessage(100L, delivery, counterparty);

        ReportCreateRequestDto request = new ReportCreateRequestDto(
                ReportTargetType.CHAT_MESSAGE,
                ReportReason.ABUSE,
                "욕설 메시지 신고",
                null,
                null,
                100L,
                null
        );

        given(chatMessageRepository.findById(100L)).willReturn(Optional.of(chatMessage));
        given(reportRepository.existsByReporter_IdAndTargetTypeAndTargetId(1L, ReportTargetType.CHAT_MESSAGE, 100L))
                .willReturn(false);
        given(reportRepository.saveAndFlush(any(Report.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        // when
        var response = reportService.createReport(reporter, request);

        // then
        assertThat(response).isNotNull();
        verify(reportRepository).saveAndFlush(any(Report.class));
    }

    @Test
    void createAccountReport_success() {
        // given
        Account reporter = mockAccount(1L);
        Account counterparty = mockAccount(2L);
        Delivery delivery = mockDelivery(10L, reporter, counterparty);

        ReportCreateRequestDto request = new ReportCreateRequestDto(
                ReportTargetType.ACCOUNT,
                ReportReason.ABUSE,
                "상대 사용자 신고",
                null,
                10L,
                null,
                2L
        );

        given(deliveryRepository.findById(10L)).willReturn(Optional.of(delivery));
        given(accountRepository.findById(2L)).willReturn(Optional.of(counterparty));
        given(reportRepository.existsByReporter_IdAndTargetTypeAndTargetId(1L, ReportTargetType.ACCOUNT, 2L))
                .willReturn(false);
        given(reportRepository.saveAndFlush(any(Report.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        // when
        var response = reportService.createReport(reporter, request);

        // then
        assertThat(response).isNotNull();
        verify(reportRepository).saveAndFlush(any(Report.class));
    }

    @Test
    void createReport_success_withImages_finalizeToReportImagesDirectory() {
        // given
        Account reporter = mockAccount(1L);
        Account counterparty = mockAccount(2L);
        Delivery delivery = mockDelivery(10L, reporter, counterparty);

        List<String> imageKeys = List.of(
                "uploads/images/123e4567-e89b-12d3-a456-426614174000.jpg",
                "uploads/images/123e4567-e89b-12d3-a456-426614174001.jpg"
        );

        ReportCreateRequestDto request = new ReportCreateRequestDto(
                ReportTargetType.DELIVERY,
                ReportReason.FRAUD,
                "증빙 이미지 첨부",
                imageKeys,
                10L,
                null,
                null
        );

        given(deliveryRepository.findById(10L)).willReturn(Optional.of(delivery));
        given(reportRepository.existsByReporter_IdAndTargetTypeAndTargetId(1L, ReportTargetType.DELIVERY, 10L))
                .willReturn(false);
        given(reportRepository.saveAndFlush(any(Report.class)))
                .willAnswer(invocation -> invocation.getArgument(0));
        given(reportImageRepository.save(any(ReportImage.class)))
                .willAnswer(invocation -> invocation.getArgument(0));
        given(s3Service.finalizeUploadedImage(anyString(), eq("report-images/")))
                .willReturn("report-images/final-image-1.jpg", "report-images/final-image-2.jpg");

        // when
        var response = reportService.createReport(reporter, request);

        // then
        assertThat(response).isNotNull();
        verify(s3Service, times(2))
                .finalizeUploadedImage(anyString(), eq("report-images/"));
        verify(reportImageRepository, times(2)).save(any(ReportImage.class));
    }

    @Test
    void createReport_fail_whenOtherWithoutDetail() {
        // given
        Account reporter = mockAccount(1L);

        ReportCreateRequestDto request = new ReportCreateRequestDto(
                ReportTargetType.DELIVERY,
                ReportReason.OTHER,
                null,
                null,
                10L,
                null,
                null
        );

        // when & then
        assertThatThrownBy(() -> reportService.createReport(reporter, request))
                .isInstanceOf(ReportException.class)
                .extracting(e -> ((ReportException) e).getCode())
                .isEqualTo(ReportErrorCode.INVALID_REPORT_OTHER_DETAIL);
    }

    @Test
    void createReport_fail_whenImageCountExceedsLimit() {
        // given
        Account reporter = mockAccount(1L);

        ReportCreateRequestDto request = new ReportCreateRequestDto(
                ReportTargetType.DELIVERY,
                ReportReason.FRAUD,
                "이미지 너무 많음",
                List.of("1", "2", "3", "4", "5", "6"),
                10L,
                null,
                null
        );

        // when & then
        assertThatThrownBy(() -> reportService.createReport(reporter, request))
                .isInstanceOf(ReportException.class)
                .extracting(e -> ((ReportException) e).getCode())
                .isEqualTo(ReportErrorCode.INVALID_REPORT_IMAGE_COUNT);
    }

    @Test
    void createDeliveryReport_fail_whenReporterIsNotParticipant() {
        // given
        Account reporter = mockAccount(1L);
        Account counterparty = mockAccount(2L);
        Account outsider = mockAccount(999L);
        Delivery delivery = mockDelivery(10L, reporter, counterparty);

        ReportCreateRequestDto request = new ReportCreateRequestDto(
                ReportTargetType.DELIVERY,
                ReportReason.FRAUD,
                "참여자 아님",
                null,
                10L,
                null,
                null
        );

        given(deliveryRepository.findById(10L)).willReturn(Optional.of(delivery));

        // when & then
        assertThatThrownBy(() -> reportService.createReport(outsider, request))
                .isInstanceOf(ReportException.class)
                .extracting(e -> ((ReportException) e).getCode())
                .isEqualTo(ReportErrorCode.REPORT_FORBIDDEN);
    }

    @Test
    void createChatMessageReport_fail_whenReportingOwnMessage() {
        // given
        Account reporter = mockAccount(1L);
        Account counterparty = mockAccount(2L);
        Delivery delivery = mockDelivery(10L, reporter, counterparty);
        ChatMessage myMessage = mockChatMessage(101L, delivery, reporter);

        ReportCreateRequestDto request = new ReportCreateRequestDto(
                ReportTargetType.CHAT_MESSAGE,
                ReportReason.ABUSE,
                "내 메시지는 신고 불가",
                null,
                null,
                101L,
                null
        );

        given(chatMessageRepository.findById(101L)).willReturn(Optional.of(myMessage));

        // when & then
        assertThatThrownBy(() -> reportService.createReport(reporter, request))
                .isInstanceOf(ReportException.class)
                .extracting(e -> ((ReportException) e).getCode())
                .isEqualTo(ReportErrorCode.REPORT_SELF_MESSAGE_NOT_ALLOWED);
    }

    @Test
    void createAccountReport_fail_whenReportingSelf() {
        // given
        Account reporter = mockAccount(1L);
        Account counterparty = mockAccount(2L);
        Delivery delivery = mockDelivery(10L, reporter, counterparty);

        ReportCreateRequestDto request = new ReportCreateRequestDto(
                ReportTargetType.ACCOUNT,
                ReportReason.ABUSE,
                "자기 자신 신고",
                null,
                10L,
                null,
                1L
        );

        given(deliveryRepository.findById(10L)).willReturn(Optional.of(delivery));
        given(accountRepository.findById(1L)).willReturn(Optional.of(reporter));

        // when & then
        assertThatThrownBy(() -> reportService.createReport(reporter, request))
                .isInstanceOf(ReportException.class)
                .extracting(e -> ((ReportException) e).getCode())
                .isEqualTo(ReportErrorCode.REPORT_SELF_ACCOUNT_NOT_ALLOWED);
    }

    @Test
    void createReport_fail_whenAlreadyExistsByExistsCheck() {
        // given
        Account reporter = mockAccount(1L);
        Account counterparty = mockAccount(2L);
        Delivery delivery = mockDelivery(10L, reporter, counterparty);

        ReportCreateRequestDto request = new ReportCreateRequestDto(
                ReportTargetType.DELIVERY,
                ReportReason.FRAUD,
                "중복 신고",
                null,
                10L,
                null,
                null
        );

        given(deliveryRepository.findById(10L)).willReturn(Optional.of(delivery));
        given(reportRepository.existsByReporter_IdAndTargetTypeAndTargetId(1L, ReportTargetType.DELIVERY, 10L))
                .willReturn(true);

        // when & then
        assertThatThrownBy(() -> reportService.createReport(reporter, request))
                .isInstanceOf(ReportException.class)
                .extracting(e -> ((ReportException) e).getCode())
                .isEqualTo(ReportErrorCode.REPORT_ALREADY_EXISTS);

        verify(reportRepository, never()).saveAndFlush(any(Report.class));
    }

    @Test
    void createReport_fail_whenDataIntegrityViolationOccurs() {
        // given
        Account reporter = mockAccount(1L);
        Account counterparty = mockAccount(2L);
        Delivery delivery = mockDelivery(10L, reporter, counterparty);

        ReportCreateRequestDto request = new ReportCreateRequestDto(
                ReportTargetType.DELIVERY,
                ReportReason.FRAUD,
                "동시성 중복 신고",
                null,
                10L,
                null,
                null
        );

        given(deliveryRepository.findById(10L)).willReturn(Optional.of(delivery));
        given(reportRepository.existsByReporter_IdAndTargetTypeAndTargetId(1L, ReportTargetType.DELIVERY, 10L))
                .willReturn(false);
        given(reportRepository.saveAndFlush(any(Report.class)))
                .willThrow(new DataIntegrityViolationException("duplicate"));

        // when & then
        assertThatThrownBy(() -> reportService.createReport(reporter, request))
                .isInstanceOf(ReportException.class)
                .extracting(e -> ((ReportException) e).getCode())
                .isEqualTo(ReportErrorCode.REPORT_ALREADY_EXISTS);
    }

    @Test
    void getMyReports_success() {
        // given
        Account reporter = mockAccount(1L);

        Report report1 = mockReportForList(
                1L,
                ReportTargetType.DELIVERY,
                10L,
                ReportReason.FRAUD,
                "신고 1",
                ReportStatus.PENDING
        );

        Report report2 = mockReportForList(
                2L,
                ReportTargetType.ACCOUNT,
                2L,
                ReportReason.ABUSE,
                "신고 2",
                ReportStatus.RESOLVED
        );

        Page<Report> page = new PageImpl<>(
                List.of(report1, report2),
                PageRequest.of(0, 20),
                2
        );

        given(reportRepository.findAllByReporter_IdOrderByCreatedAtDesc(1L, PageRequest.of(0, 20)))
                .willReturn(page);

        // when
        MyReportListResponseDto response = reportService.getMyReports(reporter, PageRequest.of(0, 20));

        // then
        assertThat(response).isNotNull();
        assertThat(response.getReports()).hasSize(2);
        assertThat(response.getPage()).isEqualTo(0);
        assertThat(response.getSize()).isEqualTo(20);
        assertThat(response.getTotalElements()).isEqualTo(2);
    }

    @Test
    void updateReportStatus_success() {
        // given
        Report report = mock(Report.class);
        ReportStatusUpdateRequestDto request = new ReportStatusUpdateRequestDto(ReportStatus.RESOLVED);

        given(reportRepository.findById(1L)).willReturn(Optional.of(report));

        // when
        reportService.updateReportStatus(1L, request);

        // then
        verify(report).updateStatus(ReportStatus.RESOLVED);
    }

    @Test
    void updateReportStatus_fail_whenReportNotFound() {
        // given
        ReportStatusUpdateRequestDto request = new ReportStatusUpdateRequestDto(ReportStatus.RESOLVED);
        given(reportRepository.findById(anyLong())).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> reportService.updateReportStatus(1L, request))
                .isInstanceOf(ReportException.class)
                .extracting(e -> ((ReportException) e).getCode())
                .isEqualTo(ReportErrorCode.REPORT_NOT_FOUND);
    }

    private Account mockAccount(Long id) {
        Account account = mock(Account.class);
        given(account.getId()).willReturn(id);
        return account;
    }

    private Delivery mockDelivery(Long id, Account sender, Account shipper) {
        Delivery delivery = mock(Delivery.class);
        given(delivery.getId()).willReturn(id);
        given(delivery.getSender()).willReturn(sender);
        given(delivery.getShipper()).willReturn(shipper);
        return delivery;
    }

    private ChatMessage mockChatMessage(Long id, Delivery delivery, Account sender) {
        ChatMessage chatMessage = mock(ChatMessage.class);
        given(chatMessage.getId()).willReturn(id);
        given(chatMessage.getDelivery()).willReturn(delivery);
        given(chatMessage.getSender()).willReturn(sender);
        return chatMessage;
    }

    private Report mockReportForList(
            Long id,
            ReportTargetType targetType,
            Long targetId,
            ReportReason reason,
            String detail,
            ReportStatus status
    ) {
        Report report = mock(Report.class);
        given(report.getId()).willReturn(id);
        given(report.getTargetType()).willReturn(targetType);
        given(report.getTargetId()).willReturn(targetId);
        given(report.getReason()).willReturn(reason);
        given(report.getDetail()).willReturn(detail);
        given(report.getStatus()).willReturn(status);
        given(report.getCreatedAt()).willReturn(LocalDateTime.now());
        given(report.getImages()).willReturn(Collections.emptyList());
        return report;
    }
}
