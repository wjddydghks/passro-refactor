package com.passro.passrobackend.point.service;

import com.passro.passrobackend.account.entity.Account;
import com.passro.passrobackend.account.repository.AccountRepository;
import com.passro.passrobackend.delivery.entity.Delivery;
import com.passro.passrobackend.delivery.entity.DeliveryGoodInfo;
import com.passro.passrobackend.point.entity.PointLog;
import com.passro.passrobackend.point.dto.PointHistoryResponseDto;
import com.passro.passrobackend.point.enums.PointIncrementReason;
import com.passro.passrobackend.point.exception.PointException;
import com.passro.passrobackend.point.exception.code.PointErrorCode;
import com.passro.passrobackend.point.repository.PointLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PointServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private PointLogRepository pointLogRepository;

    @InjectMocks
    private PointService pointService;

    @Test
    void pointHistoryContainsCurrentPointAndLogsInRepositoryOrder() {
        Account account = account(1L, 3200L);
        Delivery recentDelivery = delivery(101L, account, null);
        Delivery oldDelivery = delivery(100L, account, null);
        PointLog recentLog = PointLog.create(
                account, recentDelivery, PointIncrementReason.DELIVERY_REFUND,
                1800L, 1400L, 3200L, "배송 취소 환불");
        PointLog oldLog = PointLog.create(
                account, oldDelivery, PointIncrementReason.DELIVERY_PAYMENT,
                -1800L, 3200L, 1400L, "배송 요청 결제");
        given(accountRepository.findById(1L)).willReturn(Optional.of(account));
        given(pointLogRepository.findAllByAccountOrderByCreatedAtDesc(account))
                .willReturn(List.of(recentLog, oldLog));

        PointHistoryResponseDto response = pointService.getPointHistory(1L);

        assertThat(response.getCurrentPoint()).isEqualTo(3200L);
        assertThat(response.getPointLogs()).hasSize(2);
        assertThat(response.getPointLogs().get(0).getDelivery().getId()).isEqualTo(101L);
        assertThat(response.getPointLogs().get(0).getDelivery().getName()).isEqualTo("배송 101");
        assertThat(response.getPointLogs().get(0).getDeltaPoint()).isEqualTo(1800L);
        assertThat(response.getPointLogs().get(1).getDelivery().getId()).isEqualTo(100L);
        assertThat(response.getPointLogs().get(1).getDeltaPoint()).isEqualTo(-1800L);
    }

    @Test
    void deliveryPaymentStoresDeltaBeforeAndAfterPoints() {
        Account sender = account(1L, 5000L);
        Delivery delivery = delivery(100L, sender, null);
        given(accountRepository.findByIdForUpdate(1L)).willReturn(Optional.of(sender));

        pointService.payForDelivery(1L, delivery, 1800L);

        assertThat(sender.getPoint()).isEqualTo(3200L);
        assertLog(PointIncrementReason.DELIVERY_PAYMENT, -1800L, 5000L, 3200L);
    }

    @Test
    void insufficientBalanceDoesNotCreateLog() {
        Account sender = account(1L, 1000L);
        Delivery delivery = delivery(100L, sender, null);
        given(accountRepository.findByIdForUpdate(1L)).willReturn(Optional.of(sender));

        assertThatThrownBy(() -> pointService.payForDelivery(1L, delivery, 1800L))
                .isInstanceOf(PointException.class)
                .extracting(exception -> ((PointException) exception).getCode())
                .isEqualTo(PointErrorCode.INSUFFICIENT_BALANCE);

        assertThat(sender.getPoint()).isEqualTo(1000L);
        verify(pointLogRepository, never()).save(any());
    }

    @Test
    void deliveryRefundStoresPositiveDelta() {
        Account sender = account(1L, 3200L);
        Delivery delivery = delivery(100L, sender, null);
        given(pointLogRepository.existsByDeliveryAndIncrementReason(
                delivery, PointIncrementReason.DELIVERY_PAYMENT)).willReturn(true);
        given(accountRepository.findByIdForUpdate(1L)).willReturn(Optional.of(sender));

        pointService.refundDelivery(1L, delivery, 1800L);

        assertThat(sender.getPoint()).isEqualTo(5000L);
        assertLog(PointIncrementReason.DELIVERY_REFUND, 1800L, 3200L, 5000L);
    }

    @Test
    void deliverySettlementCreditsShipper() {
        Account sender = account(1L, 3200L);
        Account shipper = account(2L, 100L);
        Delivery delivery = delivery(100L, sender, shipper);
        given(pointLogRepository.existsByDeliveryAndIncrementReason(
                delivery, PointIncrementReason.DELIVERY_PAYMENT)).willReturn(true);
        given(accountRepository.findByIdForUpdate(2L)).willReturn(Optional.of(shipper));

        pointService.settleDelivery(2L, delivery, 1800L);

        assertThat(shipper.getPoint()).isEqualTo(1900L);
        assertLog(PointIncrementReason.DELIVERY_SETTLEMENT, 1800L, 100L, 1900L);
    }

    private void assertLog(
            PointIncrementReason reason,
            long deltaPoint,
            long beforePoint,
            long afterPoint
    ) {
        ArgumentCaptor<PointLog> captor = ArgumentCaptor.forClass(PointLog.class);
        verify(pointLogRepository).save(captor.capture());
        PointLog log = captor.getValue();
        assertThat(log.getIncrementReason()).isEqualTo(reason);
        assertThat(log.getDeltaPoint()).isEqualTo(deltaPoint);
        assertThat(log.getBeforePoint()).isEqualTo(beforePoint);
        assertThat(log.getAfterPoint()).isEqualTo(afterPoint);
    }

    private Account account(Long id, Long point) {
        return Account.builder().id(id).point(point).build();
    }

    private Delivery delivery(Long id, Account sender, Account shipper) {
        Delivery delivery = Delivery.builder()
                .id(id)
                .sender(sender)
                .shipper(shipper)
                .build();
        delivery.attachGoodInfo(DeliveryGoodInfo.builder().name("배송 " + id).build());
        return delivery;
    }
}
