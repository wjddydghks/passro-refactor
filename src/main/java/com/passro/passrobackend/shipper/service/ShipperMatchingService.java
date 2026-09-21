package com.passro.passrobackend.shipper.service;

import com.passro.passrobackend.account.entity.Account;
import com.passro.passrobackend.account.entity.AccountPlace;
import com.passro.passrobackend.account.entity.WayPoint;
import com.passro.passrobackend.account.repository.AccountPlaceRepository;
import com.passro.passrobackend.account.repository.WayPointRepository;
import com.passro.passrobackend.delivery.entity.Delivery;
import com.passro.passrobackend.delivery.enums.DeliveryState;
import com.passro.passrobackend.delivery.repository.DeliveryRepository;
import com.passro.passrobackend.place.entity.Place;
import com.passro.passrobackend.shipper.enums.MatchingPriority;
import com.passro.passrobackend.subway.dto.SubwayRouteResponseDto;
import com.passro.passrobackend.subway.dto.SubwayStationResponseDto;
import com.passro.passrobackend.subway.service.SubwayService;
import com.passro.passrobackend.subway.service.SubwayTravelTimeCalculator;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ShipperMatchingService {

    private final DeliveryRepository deliveryRepository;
    private final AccountPlaceRepository accountPlaceRepository;
    private final WayPointRepository wayPointRepository;
    private final SubwayService subwayService;

    // 배송기사의 동선과 권역을 기반으로 5단계 우선순위 정렬 및 권역 필터링이 적용된 매칭 대기 배송 목록 조회
    public List<MatchedDelivery> listMatchRequestedWithPriority(Account shipper) {
        Optional<AccountPlace> accountPlaceOpt = accountPlaceRepository.findByAccount(shipper);
        if (accountPlaceOpt.isEmpty()) {
            // 동선 정보가 등록되지 않은 배송기사인 경우 전체 대기 목록 반환 (약관 동의 건만)
            return deliveryRepository.findAllByStatus(DeliveryState.WAIT).stream()
                    .filter(delivery -> !isSentBy(delivery, shipper))
                    .filter(delivery -> Boolean.TRUE.equals(delivery.getTerms()))
                    .map(this::estimateDeliverySafely)
                    .filter(Objects::nonNull)
                    .toList();
        }

        AccountPlace accountPlace = accountPlaceOpt.get();
        List<WayPoint> wayPoints = wayPointRepository.findAllByAccountPlaceOrderByVisitOrderAsc(accountPlace);
        List<Place> wayPointPlaces = wayPoints.stream().map(WayPoint::getPlace).toList();

        Long shipperStartId = accountPlace.getStartPlace() != null ? accountPlace.getStartPlace().getId() : null;
        Long shipperDestId = accountPlace.getDestinationPlace() != null ? accountPlace.getDestinationPlace().getId() : null;

        // 배송기사 출발역 권역 조회 (SubwayService 예외 발생 시 안전 처리)
        String shipperRegion = null;
        if (shipperStartId != null) {
            try {
                shipperRegion = subwayService.getRegionByPlaceId(shipperStartId);
            } catch (Exception e) {
                log.warn("배송기사 출발역 권역 조회 중 예외 발생: shipperId={}, error={}", shipper.getId(), e.getMessage());
            }
        }

        if (shipperRegion == null) {
            log.warn("배송기사 출발역 권역을 확인할 수 없어 매칭 대기 목록을 비웁니다: shipperId={}", shipper.getId());
            return Collections.emptyList();
        }

        // 배송기사 통과 역 경로 Place ID Set 산출 (예외 발생 시 빈 Set으로 폴백)
        Set<Long> passThroughPlaceIds = Collections.emptySet();
        if (accountPlace.getStartPlace() != null && accountPlace.getDestinationPlace() != null) {
            try {
                SubwayRouteResponseDto routeDto = subwayService.findShortestRoute(
                        accountPlace.getStartPlace(),
                        wayPointPlaces,
                        accountPlace.getDestinationPlace()
                );
                passThroughPlaceIds = routeDto.getStations().stream()
                        .map(SubwayStationResponseDto::getId)
                        .collect(Collectors.toSet());
            } catch (Exception e) {
                log.warn("배송기사 최단 경로 계산 실패 (통과 역 기준 알고리즘 제외): shipperId={}, error={}", shipper.getId(), e.getMessage());
            }
        }

        // 알고리즘 판단 및 매칭 대기 배송 목록 DB 조회
        List<Delivery> pendingDeliveries = deliveryRepository.findAllByStatus(DeliveryState.WAIT);

        final String targetRegion = shipperRegion;
        final Set<Long> finalPassThroughIds = passThroughPlaceIds;

        return pendingDeliveries.stream()
                .filter(delivery -> !isSentBy(delivery, shipper))
                .map(delivery -> evaluateDeliverySafely(delivery, shipperStartId, shipperDestId, finalPassThroughIds, targetRegion))
                .filter(Objects::nonNull) // SubwayService 연산 중 예외 발생건 또는 권역 불일치건 건너뜀
                .sorted(Comparator
                        .comparingInt(EvaluatedDelivery::priorityRank)
                        .thenComparingInt(EvaluatedDelivery::sortingWeight)
                        .thenComparing(ed -> ed.delivery().getCreatedAt(), Comparator.reverseOrder())
                )
                .map(evaluated -> new MatchedDelivery(
                        evaluated.delivery(), SubwayTravelTimeCalculator.toEstimatedTimeMinutes(
                                evaluated.routeWeight())))
                .toList();
    }

    private boolean isSentBy(Delivery delivery, Account account) {
        return delivery != null
                && delivery.getSender() != null
                && delivery.getSender().getId() != null
                && account != null
                && delivery.getSender().getId().equals(account.getId());
    }

    private MatchedDelivery estimateDeliverySafely(Delivery delivery) {
        if (delivery == null || delivery.getOrigin() == null || delivery.getDest() == null) {
            return null;
        }

        try {
            int routeWeight = subwayService.findShortestRoute(
                    delivery.getOrigin(), null, delivery.getDest()).getShortestDistance();
            return new MatchedDelivery(
                    delivery, SubwayTravelTimeCalculator.toEstimatedTimeMinutes(routeWeight));
        } catch (Exception e) {
            log.warn("예상시간 계산 중 예외가 발생하여 배송건을 건너뜁니다 - Delivery ID: {}, Error: {}",
                    delivery.getId(), e.getMessage());
            return null;
        }
    }

    // SubwayService 연산 시 예외가 발생하면 해당 배송건을 건너뛰도록 null 반환
    private EvaluatedDelivery evaluateDeliverySafely(Delivery delivery, Long shipperStartId, Long shipperDestId, Set<Long> passThroughPlaceIds, String shipperRegion) {
        if (delivery == null || delivery.getOrigin() == null || delivery.getDest() == null || !Boolean.TRUE.equals(delivery.getTerms())) {
            return null;
        }

        try {
            // 1) 권역 필터링 (배송기사 출발역의 권역과 발송자 출발역의 권역 비교)
            String originRegion = subwayService.getRegionByPlaceId(delivery.getOrigin().getId());
            if (shipperRegion != null && !shipperRegion.equals(originRegion)) {
                return null; // 권역 불일치 시 건너뜀
            }

            // 2) 우선순위 5단계 판별
            MatchingPriority priority = evaluatePriority(delivery, shipperStartId, shipperDestId, passThroughPlaceIds);

            // 3) 예상시간 및 5순위 정렬에 사용할 배송 경로 가중치 산출
            int routeWeight = subwayService.findShortestRoute(
                    delivery.getOrigin(), null, delivery.getDest()).getShortestDistance();
            int sortingWeight = priority == MatchingPriority.RANK_5 ? routeWeight : 0;

            return new EvaluatedDelivery(delivery, priority.getRank(), sortingWeight, routeWeight);
        } catch (Exception e) {
            log.warn("SubwayService 연산 중 예외 발생으로 해당 배송건 건너뜀 - Delivery ID: {}, Error: {}", delivery.getId(), e.getMessage());
            return null;
        }
    }

    // 우선순위 5단계 판별 로직
    private MatchingPriority evaluatePriority(Delivery delivery, Long shipperStartId, Long shipperDestId, Set<Long> passThroughPlaceIds) {
        Long originId = delivery.getOrigin().getId();
        Long destId = delivery.getDest().getId();

        boolean originMatch = shipperStartId != null && originId.equals(shipperStartId);
        boolean destMatch = shipperDestId != null && destId.equals(shipperDestId);

        // 1순위: 출발지 & 목적지 둘 다 같음
        if (originMatch && destMatch) {
            return MatchingPriority.RANK_1;
        }

        // 2순위: 둘 중 하나만 같음
        if (originMatch || destMatch) {
            return MatchingPriority.RANK_2;
        }

        boolean originInPath = passThroughPlaceIds.contains(originId);
        boolean destInPath = passThroughPlaceIds.contains(destId);

        // 3순위: 통과 역에 출발지, 목적지 둘 다 있음
        if (originInPath && destInPath) {
            return MatchingPriority.RANK_3;
        }

        // 4순위: 통과 역에 출발지, 목적지 하나만 있음
        if (originInPath || destInPath) {
            return MatchingPriority.RANK_4;
        }

        // 5순위: 나머지 (가중치 오름차순)
        return MatchingPriority.RANK_5;
    }

    public record MatchedDelivery(Delivery delivery, int estimatedTimeMinutes) {}

    private record EvaluatedDelivery(
            Delivery delivery,
            int priorityRank,
            int sortingWeight,
            int routeWeight) {}
}
