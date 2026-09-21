package com.passro.passrobackend.shipper.service;

import com.passro.passrobackend.account.entity.Account;
import com.passro.passrobackend.account.entity.AccountPlace;
import com.passro.passrobackend.account.repository.AccountPlaceRepository;
import com.passro.passrobackend.chat.service.ChatService;
import com.passro.passrobackend.delivery.entity.Delivery;
import com.passro.passrobackend.delivery.entity.DeliveryLog;
import com.passro.passrobackend.delivery.enums.DeliveryLogType;
import com.passro.passrobackend.delivery.enums.DeliveryState;
import com.passro.passrobackend.delivery.event.DeliveryLogEvent;
import com.passro.passrobackend.delivery.exception.DeliveryException;
import com.passro.passrobackend.delivery.exception.code.DeliveryErrorCode;
import com.passro.passrobackend.delivery.repository.DeliveryLogRepository;
import com.passro.passrobackend.delivery.repository.DeliveryRepository;
import com.passro.passrobackend.file.service.S3Service;
import com.passro.passrobackend.notification.enums.NotificationType;
import com.passro.passrobackend.notification.enums.ResourceType;
import com.passro.passrobackend.notification.service.NotificationService;
import com.passro.passrobackend.shipper.dto.ShipperDeliveryDetailDto;
import com.passro.passrobackend.shipper.dto.ShipperDeliveryListDto;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ShipperService {

    private final DeliveryRepository deliveryRepository;
    private final DeliveryLogRepository deliveryLogRepository;
    private final AccountPlaceRepository accountPlaceRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final S3Service s3Service;
    private final NotificationService notificationService;
    private final ChatService chatService;

    public List<Delivery> listAllByShipper(Account account, DeliveryState status) {
        return status == null
                ? deliveryRepository.findAllByShipper(account)
                : deliveryRepository.findAllByShipperAndStatus(account, status);
    }

    public ShipperDeliveryDetailDto getDeliveryById(Account shipper, Long id) {
        Delivery delivery = getDelivery(id);
//        validateAssignedShipper(delivery, shipper);
        List<DeliveryLog> logs = deliveryLogRepository.findAllByDeliveryOrderByCreatedAtAsc(delivery);
        AccountPlace senderAccountPlace = findAccountPlace(delivery.getSender());
        AccountPlace shipperAccountPlace = findAccountPlace(delivery.getShipper());
        return ShipperDeliveryDetailDto.fromDelivery(
                delivery, logs, senderAccountPlace, shipperAccountPlace,
                this::imageUrl);
    }

    public ShipperDeliveryListDto toDeliveryListDto(Delivery delivery) {
        return toDeliveryListDto(delivery, null);
    }

    public ShipperDeliveryListDto toDeliveryListDto(
            Delivery delivery, Integer estimatedTimeMinutes) {
        return ShipperDeliveryListDto.fromDelivery(
                delivery, estimatedTimeMinutes, this::imageUrl);
    }

    private String imageUrl(String imageKey) {
        if (imageKey == null || imageKey.isBlank()) {
            return null;
        }
        return s3Service.getPresignedDownloadUrlString(imageKey);
    }

    private AccountPlace findAccountPlace(Account account) {
        if (account == null) {
            return null;
        }
        return accountPlaceRepository.findByAccount(account).orElse(null);
    }

    public List<Delivery> listMatchRequested() {
        return deliveryRepository.findAllByStatus(DeliveryState.WAIT);
    }

    @Transactional
    public void matchAccept(Account shipper, Long id) {
        Delivery delivery = getDeliveryForUpdate(id);
        validateNotOwnDelivery(delivery, shipper);
        validateStatus(delivery, DeliveryState.WAIT);
        if (delivery.getShipper() != null || !Boolean.TRUE.equals(delivery.getTerms())) {
            throw new DeliveryException(DeliveryErrorCode.INVALID_STATUS_TRANSITION);
        }

        // 매칭된 상태로 변경
        delivery.setShipper(shipper);
        delivery.setStatus(DeliveryState.MATCHED);
        deliveryRepository.save(delivery);
        eventPublisher.publishEvent(new DeliveryLogEvent(delivery, DeliveryLogType.MATCHED));
        publishDeliveryNotification(
                delivery.getSender(),
                delivery,
                "배송 매칭 완료",
                "배송기사가 배정되었습니다.");
    }

    private void validateNotOwnDelivery(Delivery delivery, Account shipper) {
        if (delivery.getSender() != null
                && delivery.getSender().getId() != null
                && shipper != null
                && delivery.getSender().getId().equals(shipper.getId())) {
            throw new DeliveryException(DeliveryErrorCode.SELF_DELIVERY_NOT_ALLOWED);
        }
    }

    @Transactional
    public void acquireAccept(Account shipper, Long id) {
        acquireAccept(shipper, id, null);
    }

    @Transactional
    public void acquireAccept(Account shipper, Long id, String imageKey) {
        Delivery delivery = getDeliveryForUpdate(id);
        validateAssignedShipper(delivery, shipper);
        validateStatus(delivery, DeliveryState.MATCHED);

        String image = imageKey == null || imageKey.isBlank()
                ? null
                : validateUploadedImage(imageKey);

        delivery.setStatus(DeliveryState.DELIVERING);
        deliveryRepository.save(delivery);
        eventPublisher.publishEvent(new DeliveryLogEvent(delivery, DeliveryLogType.PICKED_UP, image));
        chatService.sendDeliveryStatusMessage(
                delivery,
                shipper,
                "전달자가 물품을 인수했어요!",
                image);
        publishDeliveryNotification(
                delivery.getSender(),
                delivery,
                "물품 인수 완료",
                "배송기사가 물품을 인수하여 배송을 시작했습니다.");
    }

    @Transactional
    public void acquireConfirm(Account shipper, Long id) {
        acquireConfirm(shipper, id, null);
    }

    @Transactional
    public void acquireConfirm(Account shipper, Long id, String imageKey) {
        Delivery delivery = getDeliveryForUpdate(id);
        validateAssignedShipper(delivery, shipper);
        validateStatus(delivery, DeliveryState.DELIVERING);

        String image = imageKey == null || imageKey.isBlank()
                ? null
                : validateUploadedImage(imageKey);

        delivery.setStatus(DeliveryState.CONFIRM_REQUESTED);
        deliveryRepository.save(delivery);
        eventPublisher.publishEvent(new DeliveryLogEvent(delivery, DeliveryLogType.DELIVERED, image));
        chatService.sendDeliveryStatusMessage(
                delivery,
                shipper,
                "전달자가 물품 전달을 완료했어요!",
                image);
        publishDeliveryNotification(
                delivery.getSender(),
                delivery,
                "배송 완료 확인 요청",
                "배송기사가 배송을 완료했습니다. 물품을 확인해 주세요.");
    }

    private Delivery getDelivery(Long id) {
        return deliveryRepository.findById(id)
                .orElseThrow(() -> new DeliveryException(DeliveryErrorCode.NOT_FOUND));
    }

    private Delivery getDeliveryForUpdate(Long id) {
        return deliveryRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new DeliveryException(DeliveryErrorCode.NOT_FOUND));
    }

    private void validateAssignedShipper(Delivery delivery, Account shipper) {
        if (delivery.getShipper() == null || shipper == null
                || !delivery.getShipper().getId().equals(shipper.getId())) {
            throw new DeliveryException(DeliveryErrorCode.FORBIDDEN_ACCESS);
        }
    }

    private void validateStatus(Delivery delivery, DeliveryState expected) {
        if (delivery.getStatus() != expected) {
            throw new DeliveryException(DeliveryErrorCode.INVALID_STATUS_TRANSITION);
        }
    }

    private String validateUploadedImage(String imageKey) {
        return s3Service.finalizeUploadedImage(imageKey, "delivery-images/");
    }

    private void publishDeliveryNotification(
            Account recipient,
            Delivery delivery,
            String title,
            String content) {
        notificationService.publish(
                recipient,
                NotificationType.DELIVERY,
                title,
                content,
                ResourceType.DELIVERY,
                delivery.getId());
    }
}
