package com.passro.passrobackend.shipper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.passro.passrobackend.account.entity.Account;
import com.passro.passrobackend.account.entity.AccountPlace;
import com.passro.passrobackend.account.repository.AccountPlaceRepository;
import com.passro.passrobackend.account.repository.WayPointRepository;
import com.passro.passrobackend.delivery.entity.Delivery;
import com.passro.passrobackend.delivery.enums.DeliveryState;
import com.passro.passrobackend.delivery.repository.DeliveryRepository;
import com.passro.passrobackend.place.entity.Place;
import com.passro.passrobackend.shipper.service.ShipperMatchingService;
import com.passro.passrobackend.shipper.service.ShipperMatchingService.MatchedDelivery;
import com.passro.passrobackend.subway.dto.SubwayRouteResponseDto;
import com.passro.passrobackend.subway.dto.SubwayStationResponseDto;
import com.passro.passrobackend.subway.service.SubwayService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ShipperMatchingServiceTest {

    @Mock
    private DeliveryRepository deliveryRepository;

    @Mock
    private AccountPlaceRepository accountPlaceRepository;

    @Mock
    private WayPointRepository wayPointRepository;

    @Mock
    private SubwayService subwayService;

    @InjectMocks
    private ShipperMatchingService shipperMatchingService;

    @Test
    @DisplayName("권역 필터링 및 5단계 우선순위 정렬(Rank 1~5, 5순위 거리순)이 정확하게 동작한다")
    void shouldSortDeliveriesByFiveTierPriorityAndRegionFilter() {
        // Given
        Account shipper = Account.builder().id(1L).nickname("shipper").build();

        Place startPlace = Place.builder().id(10L).subwayRouteName("4호선").subwayStationName("서울역").build();
        Place destPlace = Place.builder().id(20L).subwayRouteName("2호선").subwayStationName("강남역").build();
        Place passThroughPlace1 = Place.builder().id(11L).subwayRouteName("4호선").subwayStationName("사당역").build();
        Place passThroughPlace2 = Place.builder().id(12L).subwayRouteName("4호선").subwayStationName("선바위역").build();
        Place outsidePlace1 = Place.builder().id(50L).subwayRouteName("1호선").subwayStationName("수원역").build();
        Place outsidePlace2 = Place.builder().id(60L).subwayRouteName("1호선").subwayStationName("병점역").build();
        Place outsidePlace3 = Place.builder().id(70L).subwayRouteName("1호선").subwayStationName("천안역").build();
        Place busanPlace = Place.builder().id(100L).subwayRouteName("부산1호선").subwayStationName("서면역").build();

        AccountPlace accountPlace = AccountPlace.builder()
                .account(shipper)
                .startPlace(startPlace)
                .destinationPlace(destPlace)
                .build();

        given(accountPlaceRepository.findByAccount(shipper)).willReturn(Optional.of(accountPlace));
        given(wayPointRepository.findAllByAccountPlaceOrderByVisitOrderAsc(accountPlace)).willReturn(List.of());

        // 배송기사 출발역 권역: 수도권
        given(subwayService.getRegionByPlaceId(10L)).willReturn("수도권");
        given(subwayService.getRegionByPlaceId(11L)).willReturn("수도권");
        given(subwayService.getRegionByPlaceId(50L)).willReturn("수도권");
        given(subwayService.getRegionByPlaceId(70L)).willReturn("수도권");
        given(subwayService.getRegionByPlaceId(100L)).willReturn("부산"); // 권역 불일치

        // 배송기사 이동 경로 통과 역: 10L, 11L, 12L, 20L
        SubwayRouteResponseDto shipperRoute = new SubwayRouteResponseDto(
                10, 1, List.of(
                new SubwayStationResponseDto(10L, "수도권", "4호선", "서울역"),
                new SubwayStationResponseDto(11L, "수도권", "4호선", "사당역"),
                new SubwayStationResponseDto(12L, "수도권", "4호선", "선바위역"),
                new SubwayStationResponseDto(20L, "수도권", "2호선", "강남역")
        ));
        given(subwayService.findShortestRoute(startPlace, List.of(), destPlace)).willReturn(shipperRoute);

        // 배송 대기 목록 생성
        // 1순위: 출발(10L), 목적(20L) 둘 다 같음
        Delivery rank1 = Delivery.builder().id(1L).origin(startPlace).dest(destPlace).status(DeliveryState.WAIT).terms(true).createdAt(LocalDateTime.now()).build();
        // 2순위: 출발(10L)만 같음
        Delivery rank2 = Delivery.builder().id(2L).origin(startPlace).dest(outsidePlace1).status(DeliveryState.WAIT).terms(true).createdAt(LocalDateTime.now()).build();
        // 3순위: 통과역(11L), 통과역(12L) 둘 다 경로 내에 있음
        Delivery rank3 = Delivery.builder().id(3L).origin(passThroughPlace1).dest(passThroughPlace2).status(DeliveryState.WAIT).terms(true).createdAt(LocalDateTime.now()).build();
        // 4순위: 통과역(11L) 1개만 경로 내에 있음
        Delivery rank4 = Delivery.builder().id(4L).origin(passThroughPlace1).dest(outsidePlace1).status(DeliveryState.WAIT).terms(true).createdAt(LocalDateTime.now()).build();
        // 5순위(거리 15): 둘 다 경로에 없음
        Delivery rank5Far = Delivery.builder().id(5L).origin(outsidePlace1).dest(outsidePlace2).status(DeliveryState.WAIT).terms(true).createdAt(LocalDateTime.now()).build();
        // 5순위(거리 5): 둘 다 경로에 없음 (거리 더 가까움)
        Delivery rank5Near = Delivery.builder().id(6L).origin(outsidePlace3).dest(outsidePlace2).status(DeliveryState.WAIT).terms(true).createdAt(LocalDateTime.now()).build();
        // 부산 권역: 필터링되어 제외되어야 함
        Delivery busanDelivery = Delivery.builder().id(7L).origin(busanPlace).dest(outsidePlace1).status(DeliveryState.WAIT).terms(true).createdAt(LocalDateTime.now()).build();

        given(deliveryRepository.findAllByStatus(DeliveryState.WAIT))
                .willReturn(List.of(rank5Far, busanDelivery, rank4, rank1, rank3, rank5Near, rank2));

        // 5순위 거리 계산 Mocking
        given(subwayService.findShortestRoute(outsidePlace1, null, outsidePlace2))
                .willReturn(new SubwayRouteResponseDto(15, 0, List.of()));
        given(subwayService.findShortestRoute(outsidePlace3, null, outsidePlace2))
                .willReturn(new SubwayRouteResponseDto(5, 0, List.of()));
        given(subwayService.findShortestRoute(startPlace, null, destPlace))
                .willReturn(new SubwayRouteResponseDto(10, 0, List.of()));
        given(subwayService.findShortestRoute(startPlace, null, outsidePlace1))
                .willReturn(new SubwayRouteResponseDto(12, 0, List.of()));
        given(subwayService.findShortestRoute(passThroughPlace1, null, passThroughPlace2))
                .willReturn(new SubwayRouteResponseDto(4, 0, List.of()));
        given(subwayService.findShortestRoute(passThroughPlace1, null, outsidePlace1))
                .willReturn(new SubwayRouteResponseDto(8, 0, List.of()));

        // When
        List<MatchedDelivery> result = shipperMatchingService.listMatchRequestedWithPriority(shipper);

        // Then
        // 1. 부산 권역 배송건(ID 7)은 제외되어 총 6건이어야 함
        assertThat(result).hasSize(6);
        // 2. 정렬 순서 검증: Rank 1 -> Rank 2 -> Rank 3 -> Rank 4 -> Rank 5 (Near, distance 5) -> Rank 5 (Far, distance 15)
        assertThat(result).extracting(resultItem -> resultItem.delivery().getId())
                .containsExactly(1L, 2L, 3L, 4L, 6L, 5L);
        assertThat(result).extracting(MatchedDelivery::estimatedTimeMinutes)
                .containsExactly(30, 36, 12, 24, 15, 45);
    }

    @Test
    @DisplayName("SubwayService 연산 중 예외가 발생하더라도 전체 로직이 중단되지 않고 해당 배송건만 건너뛴다")
    void shouldSkipDeliveryWhenSubwayServiceThrowsException() {
        // Given
        Account shipper = Account.builder().id(1L).build();
        Place startPlace = Place.builder().id(10L).build();
        Place destPlace = Place.builder().id(20L).build();
        Place errorPlace = Place.builder().id(999L).build();

        AccountPlace accountPlace = AccountPlace.builder()
                .account(shipper)
                .startPlace(startPlace)
                .destinationPlace(destPlace)
                .build();

        given(accountPlaceRepository.findByAccount(shipper)).willReturn(Optional.of(accountPlace));
        given(wayPointRepository.findAllByAccountPlaceOrderByVisitOrderAsc(accountPlace)).willReturn(List.of());

        given(subwayService.getRegionByPlaceId(10L)).willReturn("수도권");
        given(subwayService.getRegionByPlaceId(999L)).willThrow(new IllegalStateException("지하철 노드 정보 없음"));

        SubwayRouteResponseDto shipperRoute = new SubwayRouteResponseDto(
                10, 0, List.of(
                new SubwayStationResponseDto(10L, "수도권", "1호선", "A역"),
                new SubwayStationResponseDto(20L, "수도권", "1호선", "B역")
        ));
        given(subwayService.findShortestRoute(startPlace, List.of(), destPlace)).willReturn(shipperRoute);
        given(subwayService.findShortestRoute(startPlace, null, destPlace))
                .willReturn(new SubwayRouteResponseDto(7, 0, List.of()));

        Delivery validDelivery = Delivery.builder().id(1L).origin(startPlace).dest(destPlace).status(DeliveryState.WAIT).terms(true).build();
        Delivery errorDelivery = Delivery.builder().id(2L).origin(errorPlace).dest(destPlace).status(DeliveryState.WAIT).terms(true).build();

        given(deliveryRepository.findAllByStatus(DeliveryState.WAIT)).willReturn(List.of(errorDelivery, validDelivery));

        // When
        List<MatchedDelivery> result = shipperMatchingService.listMatchRequestedWithPriority(shipper);

        // Then
        // 예외가 발생한 ID 2번 배송건은 건너뛰고 정상건 1번만 반환됨
        assertThat(result).hasSize(1);
        assertThat(result.get(0).delivery().getId()).isEqualTo(1L);
        assertThat(result.get(0).estimatedTimeMinutes()).isEqualTo(21);
    }

    @Test
    @DisplayName("배송기사의 동선 정보(AccountPlace)가 미등록된 경우 대기 목록 전체를 그대로 반환한다")
    void shouldReturnAllPendingDeliveriesWhenShipperHasNoAccountPlace() {
        // Given
        Account shipper = Account.builder().id(1L).build();
        given(accountPlaceRepository.findByAccount(shipper)).willReturn(Optional.empty());

        Place origin = Place.builder().id(10L).build();
        Place destination = Place.builder().id(20L).build();
        Delivery delivery1 = Delivery.builder().id(1L).origin(origin).dest(destination).status(DeliveryState.WAIT).terms(true).build();
        Delivery delivery2 = Delivery.builder().id(2L).origin(origin).dest(destination).status(DeliveryState.WAIT).terms(true).build();
        given(deliveryRepository.findAllByStatus(DeliveryState.WAIT)).willReturn(List.of(delivery1, delivery2));
        given(subwayService.findShortestRoute(origin, null, destination))
                .willReturn(new SubwayRouteResponseDto(6, 0, List.of()));

        // When
        List<MatchedDelivery> result = shipperMatchingService.listMatchRequestedWithPriority(shipper);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).extracting(MatchedDelivery::delivery)
                .containsExactly(delivery1, delivery2);
        assertThat(result).extracting(MatchedDelivery::estimatedTimeMinutes)
                .containsExactly(18, 18);
    }

    @Test
    @DisplayName("배송기사가 직접 발송한 배송은 매칭 대기 목록에서 제외한다")
    void shouldExcludeDeliverySentByShipper() {
        Account shipper = Account.builder().id(1L).build();
        Account anotherSender = Account.builder().id(2L).build();
        Place origin = Place.builder().id(10L).build();
        Place destination = Place.builder().id(20L).build();
        Delivery ownDelivery = Delivery.builder()
                .id(1L)
                .sender(shipper)
                .origin(origin)
                .dest(destination)
                .status(DeliveryState.WAIT)
                .terms(true)
                .build();
        Delivery anotherDelivery = Delivery.builder()
                .id(2L)
                .sender(anotherSender)
                .origin(origin)
                .dest(destination)
                .status(DeliveryState.WAIT)
                .terms(true)
                .build();
        given(accountPlaceRepository.findByAccount(shipper)).willReturn(Optional.empty());
        given(deliveryRepository.findAllByStatus(DeliveryState.WAIT))
                .willReturn(List.of(ownDelivery, anotherDelivery));
        given(subwayService.findShortestRoute(origin, null, destination))
                .willReturn(new SubwayRouteResponseDto(6, 0, List.of()));

        List<MatchedDelivery> result = shipperMatchingService.listMatchRequestedWithPriority(shipper);

        assertThat(result).extracting(MatchedDelivery::delivery)
                .containsExactly(anotherDelivery);
    }

    @Test
    @DisplayName("배송기사의 출발역 권역 조회 시 예외가 발생하거나 null이면 매칭 대기 목록을 빈 리스트로 반환한다")
    void shouldReturnEmptyListWhenShipperRegionLookupFails() {
        // Given
        Account shipper = Account.builder().id(1L).build();
        Place startPlace = Place.builder().id(10L).build();

        AccountPlace accountPlace = AccountPlace.builder()
                .account(shipper)
                .startPlace(startPlace)
                .build();

        given(accountPlaceRepository.findByAccount(shipper)).willReturn(Optional.of(accountPlace));
        given(wayPointRepository.findAllByAccountPlaceOrderByVisitOrderAsc(accountPlace)).willReturn(List.of());
        given(subwayService.getRegionByPlaceId(10L)).willThrow(new IllegalStateException("출발역 권역 정보 없음"));

        // When
        List<MatchedDelivery> result = shipperMatchingService.listMatchRequestedWithPriority(shipper);

        // Then
        assertThat(result).isEmpty();
    }
}
