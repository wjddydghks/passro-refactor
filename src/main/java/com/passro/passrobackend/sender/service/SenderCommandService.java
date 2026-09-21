package com.passro.passrobackend.sender.service;

import com.passro.passrobackend.account.entity.Account;
import com.passro.passrobackend.delivery.configuration.DeliveryPointProperties;
import com.passro.passrobackend.delivery.entity.Delivery;
import com.passro.passrobackend.delivery.entity.DeliveryPoint;
import com.passro.passrobackend.delivery.enums.DeliveryLogType;
import com.passro.passrobackend.delivery.event.DeliveryLogEvent;
import com.passro.passrobackend.delivery.exception.DeliveryException;
import com.passro.passrobackend.delivery.exception.code.DeliveryErrorCode;
import com.passro.passrobackend.delivery.repository.DeliveryRepository;
import com.passro.passrobackend.file.service.S3Service;
import com.passro.passrobackend.notification.enums.NotificationType;
import com.passro.passrobackend.notification.enums.ResourceType;
import com.passro.passrobackend.notification.service.NotificationService;
import com.passro.passrobackend.delivery.enums.DeliveryState;
import com.passro.passrobackend.delivery.entity.DeliveryGoodInfo;
import com.passro.passrobackend.place.entity.Place;
import com.passro.passrobackend.place.repository.PlaceRepository;
import com.passro.passrobackend.point.service.PointService;
import com.passro.passrobackend.sender.dto.SenderDeliveryCreateRequestDto;
import com.passro.passrobackend.subway.dto.SubwayRouteResponseDto;
import com.passro.passrobackend.subway.service.SubwayService;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 발송 관련 DB 수정 (INSERT, UPDATE, DELETE) 관련 Service
@Service
@RequiredArgsConstructor
@Transactional
public class SenderCommandService {

    private final DeliveryRepository deliveryRepository;
    private final PlaceRepository placeRepository;
    private final SenderDeliveryValidator senderDeliveryValidator;
    private final SubwayService subwayService;
    private final PointService pointService;
    private final DeliveryPointProperties deliveryPointProperties;

    private final ApplicationEventPublisher eventPublisher;
    private final S3Service s3Service;
    private final NotificationService notificationService;

    // 발송 완료 처리
    public void completeDelivery(Account sender, Long deliveryId) {
        completeDelivery(sender, deliveryId, null);
    }

    public void completeDelivery(Account sender, Long deliveryId, String imageKey) {
        Delivery delivery = senderDeliveryValidator.getDeliveryForUpdateAndValidateOwnership(deliveryId, sender);

        // '검수 요청' 상태에서만 배송 완료 처리 가능
        if (delivery.getStatus() != DeliveryState.CONFIRM_REQUESTED) {
            throw new DeliveryException(DeliveryErrorCode.INVALID_STATUS_FOR_COMPLETION);
        }
        if (delivery.getShipper() == null) {
            throw new DeliveryException(DeliveryErrorCode.INVALID_STATUS_FOR_COMPLETION);
        }

        long settlementPoint = getTotalPoint(delivery);
        pointService.settleDelivery(delivery.getShipper().getId(), delivery, settlementPoint);

        String image = imageKey == null || imageKey.isBlank()
                ? null
                : validateUploadedImage(imageKey);

        delivery.setStatus(DeliveryState.DELIVERED);
        deliveryRepository.save(delivery);

        // 배송 프로세스 최종 완료 처리 로그에 저장
        eventPublisher.publishEvent(new DeliveryLogEvent(delivery, DeliveryLogType.DONE, image));
        notificationService.publish(
                delivery.getShipper(),
                NotificationType.DELIVERY,
                "배송 완료",
                "발송자가 배송 완료를 승인했습니다.",
                ResourceType.DELIVERY,
                delivery.getId());
    }

    // 배송 요청 생성
    public Long createDelivery(Account sender, SenderDeliveryCreateRequestDto request) {
        // 출발역과 도착역이 동일한 경우 예외 처리
        if (request.getSourceStationId().equals(request.getDestinationStationId())) {
            throw new DeliveryException(DeliveryErrorCode.SAME_ORIGIN_DESTINATION_NOT_ALLOWED);
        }

        // 출발지 및 도착지 Place 엔티티 존재 확인
        Place origin = placeRepository.findById(request.getSourceStationId())
                .orElseThrow(() -> new DeliveryException(DeliveryErrorCode.PLACE_NOT_FOUND));
        Place dest = placeRepository.findById(request.getDestinationStationId())
                .orElseThrow(() -> new DeliveryException(DeliveryErrorCode.PLACE_NOT_FOUND));

        // 지하철 데이터베이스/그래프에 등록된 유효한 지하철역 노드인지 검증
        if (subwayService.getRegionByPlaceId(origin.getId()) == null || subwayService.getRegionByPlaceId(dest.getId()) == null) {
            throw new DeliveryException(DeliveryErrorCode.PLACE_NOT_FOUND);
        }

        // 배송 (Delivery) 엔티티 생성 및 저장
        Delivery delivery = Delivery.builder()
                .sender(sender)
                .origin(origin)
                .dest(dest)
                .memo(request.getMemo())
                .status(DeliveryState.WAIT)
                .terms(false)
                .build();

        String normalizedSize = request.getSize().toUpperCase(Locale.ROOT);
        SubwayRouteResponseDto route = subwayService.findShortestRoute(origin, List.of(), dest);
        long basePoint = deliveryPointProperties.getBase();
        long distancePoint = deliveryPointProperties.pointForRoute(route.getTravelStationCount());
        long weightPoint = deliveryPointProperties.pointForSize(normalizedSize);
        long totalPoint = Math.addExact(Math.addExact(basePoint, distancePoint), weightPoint);

        // 배송 물품 정보 (DeliveryGoodInfo) 생성 및 저장
        DeliveryGoodInfo goodInfo = DeliveryGoodInfo.builder()
                .name(request.getName())
                .price(totalPoint)
                .size(normalizedSize) // TODO: 배송 사이즈는 enum으로 관리 고려 중입니다.
                .picture(request.getPicture())
                .build();

        DeliveryPoint pointInfo = DeliveryPoint.builder()
                .base_point(basePoint)
                .distance_point(distancePoint)
                .weight_point(weightPoint)
                .build();

        delivery.attachGoodInfo(goodInfo);
        delivery.attachPoint(pointInfo);
        deliveryRepository.save(delivery);

        // 배송 요청 로그 저장
        eventPublisher.publishEvent(new DeliveryLogEvent(delivery, DeliveryLogType.SEND_REQUEST));

        // 생성된 배송 정보 id return
        return delivery.getId();
    }

    // 발송 약관 동의
    public void agreeTerms(Account sender, Long deliveryId) {
        Delivery delivery = senderDeliveryValidator.getDeliveryForUpdateAndValidateOwnership(deliveryId, sender);

        if (delivery.getStatus() != DeliveryState.WAIT) {
            throw new DeliveryException(DeliveryErrorCode.INVALID_STATUS_TRANSITION);
        }

        long paymentPoint = getTotalPoint(delivery);
        pointService.payForDelivery(sender.getId(), delivery, paymentPoint);

        delivery.setTerms(true);
        deliveryRepository.save(delivery);
    }

    // 발송 요청 취소 처리
    public void cancelDelivery(Account sender, Long deliveryId) {
        Delivery delivery = senderDeliveryValidator.getDeliveryForUpdateAndValidateOwnership(deliveryId, sender);

        // 매칭이 되었다면, 취소 할 수 없음.
        if (delivery.getStatus() != DeliveryState.WAIT) {
            throw new DeliveryException(DeliveryErrorCode.CANNOT_CANCEL);
        }

        long refundPoint = getTotalPoint(delivery);
        pointService.refundDelivery(sender.getId(), delivery, refundPoint);

        delivery.setStatus(DeliveryState.CANCEL);
        deliveryRepository.save(delivery);

        // 배송 취소 처리 내역 로그에 저장
        eventPublisher.publishEvent(new DeliveryLogEvent(delivery, DeliveryLogType.CANCELED));
    }

    private long getTotalPoint(Delivery delivery) {
        DeliveryPoint point = delivery.getDeliveryPoint();
        if (point == null) {
            throw new DeliveryException(DeliveryErrorCode.DELIVERY_POINT_NOT_FOUND);
        }

        long basePoint = point.getBase_point() == null ? 0L : point.getBase_point();
        long distancePoint = point.getDistance_point() == null ? 0L : point.getDistance_point();
        long weightPoint = point.getWeight_point() == null ? 0L : point.getWeight_point();
        return Math.addExact(Math.addExact(basePoint, distancePoint), weightPoint);
    }

    private String validateUploadedImage(String imageKey) {
        return s3Service.finalizeUploadedImage(imageKey, "delivery-images/");
    }


}
