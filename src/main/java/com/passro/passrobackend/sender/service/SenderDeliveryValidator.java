package com.passro.passrobackend.sender.service;

import com.passro.passrobackend.account.entity.Account;
import com.passro.passrobackend.delivery.entity.Delivery;
import com.passro.passrobackend.delivery.exception.DeliveryException;
import com.passro.passrobackend.delivery.exception.code.DeliveryErrorCode;
import com.passro.passrobackend.delivery.repository.DeliveryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SenderDeliveryValidator {

    private final DeliveryRepository deliveryRepository;

    // 배송 단건 조회 및 발송자 소유권 검증 (일반 조회)
    public Delivery getDeliveryAndValidateOwnership(Long deliveryId, Account sender) {
        Delivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new DeliveryException(DeliveryErrorCode.NOT_FOUND));

        if (!delivery.getSender().getId().equals(sender.getId())) {
            throw new DeliveryException(DeliveryErrorCode.FORBIDDEN_ACCESS);
        }

        return delivery;
    }

    // 배송 단건 조회 및 발송자 소유권 검증 (상태 변경 및 동시성 락 선점용 Pessimistic Lock)
    public Delivery getDeliveryForUpdateAndValidateOwnership(Long deliveryId, Account sender) {
        Delivery delivery = deliveryRepository.findByIdForUpdate(deliveryId)
                .orElseThrow(() -> new DeliveryException(DeliveryErrorCode.NOT_FOUND));

        if (!delivery.getSender().getId().equals(sender.getId())) {
            throw new DeliveryException(DeliveryErrorCode.FORBIDDEN_ACCESS);
        }

        return delivery;
    }
}
