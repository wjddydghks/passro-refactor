package com.passro.passrobackend.point.service;

import com.passro.passrobackend.account.entity.Account;
import com.passro.passrobackend.account.repository.AccountRepository;
import com.passro.passrobackend.delivery.entity.Delivery;
import com.passro.passrobackend.market.entity.Market;
import com.passro.passrobackend.point.dto.PointHistoryResponseDto;
import com.passro.passrobackend.point.dto.PointLogResponseDto;
import com.passro.passrobackend.point.entity.PointLog;
import com.passro.passrobackend.point.enums.PointIncrementReason;
import com.passro.passrobackend.point.exception.PointException;
import com.passro.passrobackend.point.exception.code.PointErrorCode;
import com.passro.passrobackend.point.repository.PointLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class PointService {

    private final AccountRepository accountRepository;
    private final PointLogRepository pointLogRepository;

    @Transactional(readOnly = true)
    public PointHistoryResponseDto getPointHistory(Long accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new PointException(PointErrorCode.ACCOUNT_NOT_FOUND));

        return PointHistoryResponseDto.builder()
                .currentPoint(account.currentPoint())
                .pointLogs(pointLogRepository.findAllByAccountOrderByCreatedAtDesc(account)
                        .stream()
                        .map(PointLogResponseDto::from)
                        .toList())
                .build();
    }

    public void payForDelivery(Long senderId, Delivery delivery, long amount) {
        validateAmount(amount);
        Account sender = getAccountForUpdate(senderId);
        ensureNotProcessed(sender, delivery, PointIncrementReason.DELIVERY_PAYMENT);

        long beforePoint = sender.currentPoint();
        if (beforePoint < amount) {
            throw new PointException(PointErrorCode.INSUFFICIENT_BALANCE);
        }
        sender.usePoint(amount);
        saveLog(sender, delivery, PointIncrementReason.DELIVERY_PAYMENT,
                Math.negateExact(amount), beforePoint, sender.currentPoint(), "배송 요청 포인트 사용");
    }

    public void refundDelivery(Long senderId, Delivery delivery, long amount) {
        validateAmount(amount);
        if (!pointLogRepository.existsByDeliveryAndIncrementReason(
                delivery, PointIncrementReason.DELIVERY_PAYMENT)) {
            return;
        }

        Account sender = getAccountForUpdate(senderId);
        ensureNotProcessed(sender, delivery, PointIncrementReason.DELIVERY_REFUND);

        long beforePoint = sender.currentPoint();
        sender.earnPoint(amount);
        saveLog(sender, delivery, PointIncrementReason.DELIVERY_REFUND,
                amount, beforePoint, sender.currentPoint(), "배송 요청 취소 포인트 환불");
    }

    public void settleDelivery(Long shipperId, Delivery delivery, long amount) {
        validateAmount(amount);
        if (!pointLogRepository.existsByDeliveryAndIncrementReason(
                delivery, PointIncrementReason.DELIVERY_PAYMENT)) {
            throw new PointException(PointErrorCode.PAYMENT_NOT_FOUND);
        }

        Account shipper = getAccountForUpdate(shipperId);
        ensureNotProcessed(shipper, delivery, PointIncrementReason.DELIVERY_SETTLEMENT);

        long beforePoint = shipper.currentPoint();
        shipper.earnPoint(amount);
        saveLog(shipper, delivery, PointIncrementReason.DELIVERY_SETTLEMENT,
                amount, beforePoint, shipper.currentPoint(), "배송 완료 포인트 정산");
    }

    public long payForMarket(Long accountId, Market market) {
        long amount = market.getPrice();
        validateAmount(amount);

        Account account = getAccountForUpdate(accountId);
        long beforePoint = account.currentPoint();
        if (beforePoint < amount) {
            throw new PointException(PointErrorCode.INSUFFICIENT_BALANCE);
        }

        account.usePoint(amount);
        pointLogRepository.save(PointLog.createMarketPurchase(
                account,
                market,
                Math.negateExact(amount),
                beforePoint,
                account.currentPoint()));
        return account.currentPoint();
    }

    private Account getAccountForUpdate(Long accountId) {
        return accountRepository.findByIdForUpdate(accountId)
                .orElseThrow(() -> new PointException(PointErrorCode.ACCOUNT_NOT_FOUND));
    }

    private void ensureNotProcessed(
            Account account,
            Delivery delivery,
            PointIncrementReason reason
    ) {
        if (pointLogRepository.existsByAccountAndDeliveryAndIncrementReason(
                account, delivery, reason)) {
            throw new PointException(PointErrorCode.ALREADY_PROCESSED);
        }
    }

    private void validateAmount(long amount) {
        if (amount <= 0) {
            throw new PointException(PointErrorCode.INVALID_AMOUNT);
        }
    }

    private void saveLog(
            Account account,
            Delivery delivery,
            PointIncrementReason reason,
            long deltaPoint,
            long beforePoint,
            long afterPoint,
            String memo
    ) {
        pointLogRepository.save(PointLog.create(
                account, delivery, reason, deltaPoint, beforePoint, afterPoint, memo));
    }
}
