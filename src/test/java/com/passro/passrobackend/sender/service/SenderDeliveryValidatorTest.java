package com.passro.passrobackend.sender.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.passro.passrobackend.account.entity.Account;
import com.passro.passrobackend.delivery.entity.Delivery;
import com.passro.passrobackend.delivery.exception.DeliveryException;
import com.passro.passrobackend.delivery.exception.code.DeliveryErrorCode;
import com.passro.passrobackend.delivery.repository.DeliveryRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SenderDeliveryValidatorTest {

    @Mock
    private DeliveryRepository deliveryRepository;

    @InjectMocks
    private SenderDeliveryValidator senderDeliveryValidator;

    @Test
    @DisplayName("본인의 배송 조회 및 검증 성공 (일반 조회)")
    void getDeliveryAndValidateOwnership_success() {
        // Given
        Account sender = Account.builder().id(1L).build();
        Delivery delivery = Delivery.builder().id(100L).sender(sender).build();

        given(deliveryRepository.findById(100L)).willReturn(Optional.of(delivery));

        // When
        Delivery result = senderDeliveryValidator.getDeliveryAndValidateOwnership(100L, sender);

        // Then
        assertThat(result).isEqualTo(delivery);
    }

    @Test
    @DisplayName("타인의 배송에 접근 시 FORBIDDEN_ACCESS 예외가 발생한다 (일반 조회)")
    void getDeliveryAndValidateOwnership_forbidden() {
        // Given
        Account sender = Account.builder().id(1L).build();
        Account stranger = Account.builder().id(2L).build();
        Delivery delivery = Delivery.builder().id(100L).sender(sender).build();

        given(deliveryRepository.findById(100L)).willReturn(Optional.of(delivery));

        // When & Then
        assertThatThrownBy(() -> senderDeliveryValidator.getDeliveryAndValidateOwnership(100L, stranger))
                .isInstanceOf(DeliveryException.class)
                .extracting(e -> ((DeliveryException) e).getCode())
                .isEqualTo(DeliveryErrorCode.FORBIDDEN_ACCESS);
    }

    @Test
    @DisplayName("존재하지 않는 배송 ID 조회 시 NOT_FOUND 예외가 발생한다")
    void getDeliveryAndValidateOwnership_notFound() {
        // Given
        Account sender = Account.builder().id(1L).build();
        given(deliveryRepository.findById(999L)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> senderDeliveryValidator.getDeliveryAndValidateOwnership(999L, sender))
                .isInstanceOf(DeliveryException.class)
                .extracting(e -> ((DeliveryException) e).getCode())
                .isEqualTo(DeliveryErrorCode.NOT_FOUND);
    }

    @Test
    @DisplayName("본인의 배송 비관적 락 조회 및 검증 성공")
    void getDeliveryForUpdateAndValidateOwnership_success() {
        // Given
        Account sender = Account.builder().id(1L).build();
        Delivery delivery = Delivery.builder().id(100L).sender(sender).build();

        given(deliveryRepository.findByIdForUpdate(100L)).willReturn(Optional.of(delivery));

        // When
        Delivery result = senderDeliveryValidator.getDeliveryForUpdateAndValidateOwnership(100L, sender);

        // Then
        assertThat(result).isEqualTo(delivery);
    }

    @Test
    @DisplayName("타인의 배송 비관적 락 접근 시 FORBIDDEN_ACCESS 예외가 발생한다")
    void getDeliveryForUpdateAndValidateOwnership_forbidden() {
        // Given
        Account sender = Account.builder().id(1L).build();
        Account stranger = Account.builder().id(2L).build();
        Delivery delivery = Delivery.builder().id(100L).sender(sender).build();

        given(deliveryRepository.findByIdForUpdate(100L)).willReturn(Optional.of(delivery));

        // When & Then
        assertThatThrownBy(() -> senderDeliveryValidator.getDeliveryForUpdateAndValidateOwnership(100L, stranger))
                .isInstanceOf(DeliveryException.class)
                .extracting(e -> ((DeliveryException) e).getCode())
                .isEqualTo(DeliveryErrorCode.FORBIDDEN_ACCESS);
    }
}
