package com.passro.passrobackend.sender.service;

import com.passro.passrobackend.account.entity.Account;
import com.passro.passrobackend.account.entity.AccountPlace;
import com.passro.passrobackend.account.repository.AccountPlaceRepository;
import com.passro.passrobackend.account.repository.WayPointRepository;
import com.passro.passrobackend.delivery.configuration.DeliveryPointProperties;
import com.passro.passrobackend.delivery.entity.Delivery;
import com.passro.passrobackend.delivery.entity.DeliveryLog;
import com.passro.passrobackend.delivery.enums.DeliveryState;
import com.passro.passrobackend.delivery.exception.DeliveryException;
import com.passro.passrobackend.delivery.exception.code.DeliveryErrorCode;
import com.passro.passrobackend.delivery.repository.DeliveryLogRepository;
import com.passro.passrobackend.delivery.repository.DeliveryRepository;
import com.passro.passrobackend.file.service.S3Service;
import com.passro.passrobackend.global.advice.code.CommonErrorCode;
import com.passro.passrobackend.place.entity.Place;
import com.passro.passrobackend.sender.dto.SenderDeliveryDetailDto;
import com.passro.passrobackend.sender.dto.SenderDeliveryListDto;
import com.passro.passrobackend.sender.dto.SenderPaymentAmountDto;
import com.passro.passrobackend.subway.code.SubwayErrorCode;
import com.passro.passrobackend.subway.dto.SubwayRouteResponseDto;
import com.passro.passrobackend.subway.service.SubwayService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

// 발송 관련 DB 조회 및 계산 Service
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SenderQueryService {

    private final DeliveryRepository deliveryRepository;
    private final DeliveryLogRepository deliveryLogRepository;
    private final SenderDeliveryValidator senderDeliveryValidator;
    private final AccountPlaceRepository accountPlaceRepository;
    private final WayPointRepository wayPointRepository;
    private final SubwayService subwayService;
    private final DeliveryPointProperties deliveryPointProperties;
    private final S3Service s3Service;

    // 발송자 배송 목록 전체 조회
    public List<SenderDeliveryListDto> getSenders(Account sender, DeliveryState status) {
        List<Delivery> deliveries = status == null
                ? deliveryRepository.findAllBySender(sender)
                : deliveryRepository.findAllBySenderAndStatus(sender, status);

        if (deliveries.isEmpty()) {
            return List.of();
        }

        return deliveries.stream()
                .map(delivery -> SenderDeliveryListDto.builder()
                    .deliveryId(delivery.getId())
                    .name(delivery.getDeliveryGoodInfo() != null
                            ? delivery.getDeliveryGoodInfo().getName()
                            : null)
                    .originPlace(delivery.getOrigin())
                    .destPlace(delivery.getDest())
                    .status(delivery.getStatus())
                    .createdAt(delivery.getCreatedAt())
                    .build())
                .toList();
    }

    // 발송 단건 상세 정보 조회
    public SenderDeliveryDetailDto getDeliveryDetail(Account sender, Long deliveryId) {
        Delivery delivery = senderDeliveryValidator.getDeliveryAndValidateOwnership(deliveryId, sender);

        // 배송 타임라인을 날짜 오름차순으로 조회
        List<DeliveryLog> logs = deliveryLogRepository.findAllByDeliveryOrderByCreatedAtAsc(delivery);
        AccountPlace shipperAccountPlace = delivery.getShipper() != null
                ? accountPlaceRepository.findByAccount(delivery.getShipper()).orElse(null)
                : null;

        return SenderDeliveryDetailDto.fromEntity(
                delivery, logs, shipperAccountPlace, this::imageUrl);
    }

    private String imageUrl(String imageKey) {
        if (imageKey == null || imageKey.isBlank()) {
            return null;
        }
        return s3Service.getPresignedDownloadUrlString(imageKey);
    }

    public SubwayRouteResponseDto getShipperCommuteRoute(Account sender, Long deliveryId) {
        Delivery delivery = senderDeliveryValidator.getDeliveryAndValidateOwnership(deliveryId, sender);
        Account shipper = delivery.getShipper();
        if (shipper == null) {
            throw new DeliveryException(DeliveryErrorCode.SHIPPER_NOT_ASSIGNED);
        }

        AccountPlace accountPlace = accountPlaceRepository.findByAccount(shipper)
                .orElseThrow(() -> new DeliveryException(DeliveryErrorCode.SHIPPER_ROUTE_NOT_FOUND));
        if (accountPlace.getStartPlace() == null || accountPlace.getDestinationPlace() == null) {
            throw new DeliveryException(DeliveryErrorCode.SHIPPER_ROUTE_NOT_FOUND);
        }

        List<Place> waypoints = wayPointRepository
                .findAllByAccountPlaceOrderByVisitOrderAsc(accountPlace)
                .stream()
                .map(wayPoint -> wayPoint.getPlace())
                .toList();
        return findRoute(accountPlace.getStartPlace(), waypoints, accountPlace.getDestinationPlace());
    }

    public SubwayRouteResponseDto getDeliveryRoute(Account sender, Long deliveryId) {
        Delivery delivery = senderDeliveryValidator.getDeliveryAndValidateOwnership(deliveryId, sender);
        return findRoute(delivery.getOrigin(), List.of(), delivery.getDest());
    }

    private SubwayRouteResponseDto findRoute(
            Place origin,
            List<Place> waypoints,
            Place destination) {
        try {
            return subwayService.findShortestRoute(origin, waypoints, destination);
        } catch (IllegalArgumentException exception) {
            throw new DeliveryException(SubwayErrorCode.PLACE_NOT_FOUND);
        } catch (IllegalStateException exception) {
            throw new DeliveryException(SubwayErrorCode.ROUTE_NOT_FOUND);
        }
    }

    // 배송 요청 생성 전, 결제 금액(포인트)을 실시간으로 계산
    public SenderPaymentAmountDto getPaymentAmount(Long sourceStationId, Long destinationStationId, String size) {
        // 필수 파라미터 (출발역, 도착역) 검증
        if (sourceStationId == null || destinationStationId == null) {
            throw new DeliveryException(DeliveryErrorCode.PLACE_NOT_FOUND);
        }

        if (sourceStationId.equals(destinationStationId)) {
            throw new DeliveryException(DeliveryErrorCode.SAME_ORIGIN_DESTINATION_NOT_ALLOWED);
        }

        // 물품 크기/무게 값 검증
        if (size == null || size.isBlank()) {
            throw new DeliveryException(CommonErrorCode.INVALID_REQUEST);
        }

        // 물품 크기 문자열 대문자 정규화 (예: "s" -> "S")
        String normalizedSize = size.toUpperCase(Locale.ROOT);

        // SubwayService를 사용하여 출발역과 도착역 간 최단 경로 탐색
        SubwayRouteResponseDto route;
        try {
            route = subwayService.findShortestRouteByPlaceIds(sourceStationId, null, destinationStationId);
        } catch (IllegalArgumentException exception) {
            // 해당 Place ID가 존재하지 않는 역인 경우
            throw new DeliveryException(DeliveryErrorCode.PLACE_NOT_FOUND);
        } catch (IllegalStateException exception) {
            // 지하철 그래프 상에서 경로를 찾을 수 없는 경우
            throw new DeliveryException(SubwayErrorCode.ROUTE_NOT_FOUND);
        }

        // 5. 이동 정거장 수 계산 (환승역 제외 순수 이동 정거장 수)
        int travelStations = route.getTravelStationCount();

        // 6. 정책 기반 항목별 결제 포인트 계산
        // (1) 기본 요금: 2,000원
        long basePoint = deliveryPointProperties.getBase();

        // (2) 거리 요금: 이동 정거장 수 기준 (10정거장 이하: 0원 / 10정거장 초과: 200원)
        long distancePoint = deliveryPointProperties.pointForRoute(travelStations);

        // (3) 무게/크기 요금: S (+0원) / M (+500원) / L (+1,000원)
        long weightPoint;
        try {
            weightPoint = deliveryPointProperties.pointForSize(normalizedSize);
        } catch (IllegalArgumentException exception) {
            // 지원하지 않는 크기 규격인 경우
            throw new DeliveryException(CommonErrorCode.INVALID_REQUEST);
        }

        // (4) 총 결제 포인트 합산 (기본 + 거리 + 무게)
        long totalPoint = Math.addExact(Math.addExact(basePoint, distancePoint), weightPoint);

        // 7. 계산 결과를 DTO로 생성하여 반환 (DB 저장은 이루어지지 않음)
        return SenderPaymentAmountDto.builder()
                .basePoint(basePoint)
                .distancePoint(distancePoint)
                .weightPoint(weightPoint)
                .totalPoint(totalPoint)
                .build();
    }
}
