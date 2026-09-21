package com.passro.passrobackend.shipper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.passro.passrobackend.account.entity.Account;
import com.passro.passrobackend.chat.service.ChatService;
import com.passro.passrobackend.delivery.entity.Delivery;
import com.passro.passrobackend.delivery.enums.DeliveryState;
import com.passro.passrobackend.delivery.exception.DeliveryException;
import com.passro.passrobackend.delivery.exception.code.DeliveryErrorCode;
import com.passro.passrobackend.delivery.repository.DeliveryRepository;
import com.passro.passrobackend.file.service.S3Service;
import com.passro.passrobackend.notification.enums.NotificationType;
import com.passro.passrobackend.notification.enums.ResourceType;
import com.passro.passrobackend.notification.service.NotificationService;
import com.passro.passrobackend.shipper.dto.ShipperDeliveryListDto;
import com.passro.passrobackend.shipper.service.ShipperService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class ShipperServiceTest {

    @Mock
    private DeliveryRepository deliveryRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private NotificationService notificationService;

    @Mock
    private S3Service s3Service;

    @Mock
    private ChatService chatService;

    @InjectMocks
    private ShipperService shipperService;

    @Test
    void listAllByShipperWithoutStatusReturnsAllAssignedDeliveries() {
        Account shipper = Account.builder().id(1L).build();
        Delivery delivery = Delivery.builder().id(10L).shipper(shipper).build();
        given(deliveryRepository.findAllByShipper(shipper)).willReturn(List.of(delivery));

        assertThat(shipperService.listAllByShipper(shipper, null)).containsExactly(delivery);
    }

    @Test
    void listAllByShipperWithStatusReturnsOnlyFilteredDeliveries() {
        Account shipper = Account.builder().id(1L).build();
        Delivery delivering = Delivery.builder()
                .id(10L)
                .shipper(shipper)
                .status(DeliveryState.DELIVERING)
                .build();
        given(deliveryRepository.findAllByShipperAndStatus(shipper, DeliveryState.DELIVERING))
                .willReturn(List.of(delivering));

        assertThat(shipperService.listAllByShipper(shipper, DeliveryState.DELIVERING))
                .containsExactly(delivering);
    }

    @Test
    void deliveryListDtoContainsCreatedAtAndEstimatedTime() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 8, 3, 11, 0);
        Delivery delivery = Delivery.builder()
                .id(10L)
                .status(DeliveryState.WAIT)
                .createdAt(createdAt)
                .build();

        ShipperDeliveryListDto result = ShipperDeliveryListDto.fromDelivery(
                delivery, 30, imageKey -> imageKey);

        assertThat(result.getCreatedAt()).isEqualTo(createdAt);
        assertThat(result.getEstimatedTimeMinutes()).isEqualTo(30);
    }

    @Test
    void shipperCannotAcceptOwnDelivery() {
        Account account = Account.builder().id(1L).build();
        Delivery delivery = Delivery.builder()
                .id(10L)
                .sender(account)
                .status(DeliveryState.WAIT)
                .terms(true)
                .build();
        given(deliveryRepository.findByIdForUpdate(10L)).willReturn(java.util.Optional.of(delivery));

        assertThatThrownBy(() -> shipperService.matchAccept(account, 10L))
                .isInstanceOf(DeliveryException.class)
                .extracting("code")
                .isEqualTo(DeliveryErrorCode.SELF_DELIVERY_NOT_ALLOWED);
    }

    @Test
    void matchingPublishesNotificationToSender() {
        Account sender = Account.builder().id(1L).build();
        Account shipper = Account.builder().id(2L).build();
        Delivery delivery = Delivery.builder()
                .id(10L)
                .sender(sender)
                .status(DeliveryState.WAIT)
                .terms(true)
                .build();
        given(deliveryRepository.findByIdForUpdate(10L)).willReturn(java.util.Optional.of(delivery));

        shipperService.matchAccept(shipper, 10L);

        assertThat(delivery.getStatus()).isEqualTo(DeliveryState.MATCHED);
        verify(notificationService).publish(
                sender,
                NotificationType.DELIVERY,
                "배송 매칭 완료",
                "배송기사가 배정되었습니다.",
                ResourceType.DELIVERY,
                10L);
    }

    @Test
    void pickupPublishesNotificationToSender() {
        Account sender = Account.builder().id(1L).build();
        Account shipper = Account.builder().id(2L).build();
        Delivery delivery = Delivery.builder()
                .id(10L)
                .sender(sender)
                .shipper(shipper)
                .status(DeliveryState.MATCHED)
                .build();
        given(deliveryRepository.findByIdForUpdate(10L)).willReturn(java.util.Optional.of(delivery));

        shipperService.acquireAccept(shipper, 10L);

        assertThat(delivery.getStatus()).isEqualTo(DeliveryState.DELIVERING);
        verify(notificationService).publish(
                sender,
                NotificationType.DELIVERY,
                "물품 인수 완료",
                "배송기사가 물품을 인수하여 배송을 시작했습니다.",
                ResourceType.DELIVERY,
                10L);
        verify(chatService).sendDeliveryStatusMessage(
                delivery,
                shipper,
                "전달자가 물품을 인수했어요!",
                null);
    }

    @Test
    void deliveryConfirmationPublishesNotificationToSender() {
        Account sender = Account.builder().id(1L).build();
        Account shipper = Account.builder().id(2L).build();
        Delivery delivery = Delivery.builder()
                .id(10L)
                .sender(sender)
                .shipper(shipper)
                .status(DeliveryState.DELIVERING)
                .build();
        given(deliveryRepository.findByIdForUpdate(10L)).willReturn(java.util.Optional.of(delivery));

        shipperService.acquireConfirm(shipper, 10L);

        assertThat(delivery.getStatus()).isEqualTo(DeliveryState.CONFIRM_REQUESTED);
        verify(notificationService).publish(
                sender,
                NotificationType.DELIVERY,
                "배송 완료 확인 요청",
                "배송기사가 배송을 완료했습니다. 물품을 확인해 주세요.",
                ResourceType.DELIVERY,
                10L);
        verify(chatService).sendDeliveryStatusMessage(
                delivery,
                shipper,
                "전달자가 물품 전달을 완료했어요!",
                null);
    }

    @Test
    void pickupImageIsSentToDeliveryChatRoom() {
        Account sender = Account.builder().id(1L).build();
        Account shipper = Account.builder().id(2L).build();
        Delivery delivery = Delivery.builder()
                .id(10L)
                .sender(sender)
                .shipper(shipper)
                .status(DeliveryState.MATCHED)
                .build();
        given(deliveryRepository.findByIdForUpdate(10L)).willReturn(java.util.Optional.of(delivery));
        given(s3Service.finalizeUploadedImage("uploads/images/pickup.png", "delivery-images/"))
                .willReturn("delivery-images/pickup-final.png");

        shipperService.acquireAccept(shipper, 10L, "uploads/images/pickup.png");

        verify(chatService).sendDeliveryStatusMessage(
                delivery,
                shipper,
                "전달자가 물품을 인수했어요!",
                "delivery-images/pickup-final.png");
    }

    @Test
    void completionImageIsSentToDeliveryChatRoom() {
        Account sender = Account.builder().id(1L).build();
        Account shipper = Account.builder().id(2L).build();
        Delivery delivery = Delivery.builder()
                .id(10L)
                .sender(sender)
                .shipper(shipper)
                .status(DeliveryState.DELIVERING)
                .build();
        given(deliveryRepository.findByIdForUpdate(10L)).willReturn(java.util.Optional.of(delivery));
        given(s3Service.finalizeUploadedImage("uploads/images/complete.png", "delivery-images/"))
                .willReturn("delivery-images/complete-final.png");

        shipperService.acquireConfirm(shipper, 10L, "uploads/images/complete.png");

        verify(chatService).sendDeliveryStatusMessage(
                delivery,
                shipper,
                "전달자가 물품 전달을 완료했어요!",
                "delivery-images/complete-final.png");
    }
}
