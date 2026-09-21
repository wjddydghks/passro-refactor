package com.passro.passrobackend.sender.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.passro.passrobackend.account.entity.Account;
import com.passro.passrobackend.account.entity.AccountPlace;
import com.passro.passrobackend.account.entity.WayPoint;
import com.passro.passrobackend.account.repository.AccountPlaceRepository;
import com.passro.passrobackend.account.repository.WayPointRepository;
import com.passro.passrobackend.delivery.configuration.DeliveryPointProperties;
import com.passro.passrobackend.delivery.entity.Delivery;
import com.passro.passrobackend.delivery.entity.DeliveryGoodInfo;
import com.passro.passrobackend.delivery.entity.DeliveryLog;
import com.passro.passrobackend.delivery.enums.DeliveryLogType;
import com.passro.passrobackend.delivery.enums.DeliveryState;
import com.passro.passrobackend.delivery.exception.DeliveryException;
import com.passro.passrobackend.delivery.exception.code.DeliveryErrorCode;
import com.passro.passrobackend.delivery.repository.DeliveryLogRepository;
import com.passro.passrobackend.delivery.repository.DeliveryRepository;
import com.passro.passrobackend.file.service.S3Service;
import com.passro.passrobackend.place.entity.Place;
import com.passro.passrobackend.sender.dto.SenderDeliveryDetailDto;
import com.passro.passrobackend.sender.dto.SenderDeliveryListDto;
import com.passro.passrobackend.sender.dto.SenderPaymentAmountDto;
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
class SenderQueryServiceTest {

    @Mock
    private DeliveryRepository deliveryRepository;

    @Mock
    private DeliveryLogRepository deliveryLogRepository;

    @Mock
    private SenderDeliveryValidator senderDeliveryValidator;

    @Mock
    private AccountPlaceRepository accountPlaceRepository;

    @Mock
    private WayPointRepository wayPointRepository;

    @Mock
    private SubwayService subwayService;

    @Mock
    private DeliveryPointProperties deliveryPointProperties;

    @Mock
    private S3Service s3Service;

    @InjectMocks
    private SenderQueryService senderQueryService;

    @Test
    @DisplayName("발송자 배송 목록 조회 시 Place 객체 및 물품명이 정상 포함되어 반환된다")
    void getSenders_success() {
        // Given
        Account sender = Account.builder().id(1L).nickname("sender").build();
        Place origin = Place.builder().id(10L).subwayRouteName("2호선").subwayStationName("강남").build();
        Place dest = Place.builder().id(20L).subwayRouteName("2호선").subwayStationName("홍대입구").build();
        LocalDateTime createdAt = LocalDateTime.of(2026, 8, 3, 10, 30);

        Delivery delivery = Delivery.builder()
                .id(100L)
                .sender(sender)
                .origin(origin)
                .dest(dest)
                .status(DeliveryState.WAIT)
                .createdAt(createdAt)
                .build();
        delivery.attachGoodInfo(DeliveryGoodInfo.builder().name("노트북").build());

        given(deliveryRepository.findAllBySender(sender)).willReturn(List.of(delivery));

        // When
        List<SenderDeliveryListDto> result = senderQueryService.getSenders(sender, null);

        // Then
        assertThat(result).hasSize(1);
        SenderDeliveryListDto dto = result.get(0);
        assertThat(dto.getDeliveryId()).isEqualTo(100L);
        assertThat(dto.getName()).isEqualTo("노트북");
        assertThat(dto.getOriginPlace()).isEqualTo(origin);
        assertThat(dto.getDestPlace()).isEqualTo(dest);
        assertThat(dto.getStatus()).isEqualTo(DeliveryState.WAIT);
        assertThat(dto.getCreatedAt()).isEqualTo(createdAt);
    }

    @Test
    @DisplayName("발송자 배송 목록이 없으면 빈 리스트를 반환한다")
    void getSenders_empty() {
        // Given
        Account sender = Account.builder().id(1L).build();
        given(deliveryRepository.findAllBySender(sender)).willReturn(List.of());

        // When
        List<SenderDeliveryListDto> result = senderQueryService.getSenders(sender, null);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("배송 상태 필터가 있으면 발송자의 해당 상태 배송만 조회한다")
    void getSenders_withStatusFilter() {
        Account sender = Account.builder().id(1L).build();
        Delivery delivered = Delivery.builder()
                .id(100L)
                .sender(sender)
                .status(DeliveryState.DELIVERED)
                .build();
        given(deliveryRepository.findAllBySenderAndStatus(sender, DeliveryState.DELIVERED))
                .willReturn(List.of(delivered));

        List<SenderDeliveryListDto> result = senderQueryService.getSenders(
                sender, DeliveryState.DELIVERED);

        assertThat(result).singleElement()
                .extracting(SenderDeliveryListDto::getStatus)
                .isEqualTo(DeliveryState.DELIVERED);
    }

    @Test
    @DisplayName("발송 배송 단건 상세 정보 조회 성공")
    void getDeliveryDetail_success() {
        // Given
        Account sender = Account.builder().id(1L).build();
        Account shipper = Account.builder().id(2L).name("배송기사").picture("profile.jpg").build();
        Place origin = Place.builder().id(10L).subwayRouteName("2호선").subwayStationName("강남").build();
        Place destination = Place.builder().id(20L).subwayRouteName("신분당선").subwayStationName("판교").build();
        Delivery delivery = Delivery.builder()
                .id(100L)
                .sender(sender)
                .shipper(shipper)
                .origin(origin)
                .dest(destination)
                .status(DeliveryState.DELIVERING)
                .build();
        delivery.attachGoodInfo(DeliveryGoodInfo.builder().name("노트북").build());

        DeliveryLog log1 = DeliveryLog.builder().id(1L).type(DeliveryLogType.SEND_REQUEST).createdAt(LocalDateTime.now()).build();
        DeliveryLog log2 = DeliveryLog.builder().id(2L).type(DeliveryLogType.MATCHED).createdAt(LocalDateTime.now()).build();

        given(senderDeliveryValidator.getDeliveryAndValidateOwnership(100L, sender)).willReturn(delivery);
        given(deliveryLogRepository.findAllByDeliveryOrderByCreatedAtAsc(delivery)).willReturn(List.of(log1, log2));
        given(s3Service.getPresignedDownloadUrlString("profile.jpg")).willReturn("profile.jpg");

        // When
        SenderDeliveryDetailDto result = senderQueryService.getDeliveryDetail(sender, 100L);

        // Then
        assertThat(result.getId()).isEqualTo(100L);
        assertThat(result.getName()).isEqualTo("노트북");
        assertThat(result.getOriginPlace()).isEqualTo(origin);
        assertThat(result.getDestPlace()).isEqualTo(destination);
        assertThat(result.getStatus()).isEqualTo(DeliveryState.DELIVERING);
        assertThat(result.getShipperInfo().getName()).isEqualTo("배송기사");
        assertThat(result.getDeliveryTimeLine()).hasSize(2);
    }

    @Test
    @DisplayName("배송에 매칭된 배송기사의 경유지를 포함한 통학 경로를 조회한다")
    void getShipperCommuteRoute_success() {
        Account sender = Account.builder().id(1L).build();
        Account shipper = Account.builder().id(2L).build();
        Place start = Place.builder().id(10L).build();
        Place waypointPlace = Place.builder().id(11L).build();
        Place destination = Place.builder().id(12L).build();
        Delivery delivery = Delivery.builder().id(100L).sender(sender).shipper(shipper).build();
        AccountPlace accountPlace = AccountPlace.builder()
                .account(shipper)
                .startPlace(start)
                .destinationPlace(destination)
                .build();
        WayPoint wayPoint = WayPoint.builder()
                .accountPlace(accountPlace)
                .place(waypointPlace)
                .visitOrder(1)
                .build();
        SubwayRouteResponseDto expected = new SubwayRouteResponseDto(2, 0, List.of());

        given(senderDeliveryValidator.getDeliveryAndValidateOwnership(100L, sender)).willReturn(delivery);
        given(accountPlaceRepository.findByAccount(shipper)).willReturn(Optional.of(accountPlace));
        given(wayPointRepository.findAllByAccountPlaceOrderByVisitOrderAsc(accountPlace))
                .willReturn(List.of(wayPoint));
        given(subwayService.findShortestRoute(start, List.of(waypointPlace), destination)).willReturn(expected);

        assertThat(senderQueryService.getShipperCommuteRoute(sender, 100L)).isSameAs(expected);
    }

    @Test
    @DisplayName("매칭되지 않은 배송의 배송기사 통학 경로 조회를 거부한다")
    void getShipperCommuteRoute_withoutShipper() {
        Account sender = Account.builder().id(1L).build();
        Delivery delivery = Delivery.builder().id(100L).sender(sender).build();
        given(senderDeliveryValidator.getDeliveryAndValidateOwnership(100L, sender)).willReturn(delivery);

        assertThatThrownBy(() -> senderQueryService.getShipperCommuteRoute(sender, 100L))
                .isInstanceOf(DeliveryException.class)
                .extracting("code")
                .isEqualTo(DeliveryErrorCode.SHIPPER_NOT_ASSIGNED);
    }

    @Test
    @DisplayName("배송에 등록된 출발역과 도착역의 경로를 조회한다")
    void getDeliveryRoute_success() {
        Account sender = Account.builder().id(1L).build();
        Place origin = Place.builder().id(10L).build();
        Place destination = Place.builder().id(20L).build();
        Delivery delivery = Delivery.builder()
                .id(100L)
                .sender(sender)
                .origin(origin)
                .dest(destination)
                .build();
        SubwayRouteResponseDto expected = new SubwayRouteResponseDto(3, 1, List.of());

        given(senderDeliveryValidator.getDeliveryAndValidateOwnership(100L, sender)).willReturn(delivery);
        given(subwayService.findShortestRoute(origin, List.of(), destination)).willReturn(expected);

        assertThat(senderQueryService.getDeliveryRoute(sender, 100L)).isSameAs(expected);
    }

    @Test
    @DisplayName("발송 금액 정보 계산 성공 - S 사이즈, 10정거장 이하 (기본 요금 2000원)")
    void getPaymentAmount_success_sizeS_under10Stations() {
        // Given
        SubwayStationResponseDto s1 = new SubwayStationResponseDto(1L, "수도권", "2호선", "역1");
        SubwayStationResponseDto s2 = new SubwayStationResponseDto(2L, "수도권", "2호선", "역2");
        SubwayRouteResponseDto route = new SubwayRouteResponseDto(1, 0, List.of(s1, s2));

        given(subwayService.findShortestRouteByPlaceIds(101L, null, 420L)).willReturn(route);
        given(deliveryPointProperties.getBase()).willReturn(2000L);
        given(deliveryPointProperties.pointForRoute(1)).willReturn(0L);
        given(deliveryPointProperties.pointForSize("S")).willReturn(0L);

        // When
        SenderPaymentAmountDto result = senderQueryService.getPaymentAmount(101L, 420L, "S");

        // Then
        assertThat(result.getBasePoint()).isEqualTo(2000L);
        assertThat(result.getDistancePoint()).isEqualTo(0L);
        assertThat(result.getWeightPoint()).isEqualTo(0L);
        assertThat(result.getTotalPoint()).isEqualTo(2000L);
    }

    @Test
    @DisplayName("발송 금액 정보 계산 성공 - L 사이즈, 10정거장 초과 (+200원, +1000원)")
    void getPaymentAmount_success_sizeL_over10Stations() {
        // Given
        List<SubwayStationResponseDto> stations = List.of(
                new SubwayStationResponseDto(1L, "수도권", "2호선", "역1"),
                new SubwayStationResponseDto(2L, "수도권", "2호선", "역2"),
                new SubwayStationResponseDto(3L, "수도권", "2호선", "역3"),
                new SubwayStationResponseDto(4L, "수도권", "2호선", "역4"),
                new SubwayStationResponseDto(5L, "수도권", "2호선", "역5"),
                new SubwayStationResponseDto(6L, "수도권", "2호선", "역6"),
                new SubwayStationResponseDto(7L, "수도권", "2호선", "역7"),
                new SubwayStationResponseDto(8L, "수도권", "2호선", "역8"),
                new SubwayStationResponseDto(9L, "수도권", "2호선", "역9"),
                new SubwayStationResponseDto(10L, "수도권", "2호선", "역10"),
                new SubwayStationResponseDto(11L, "수도권", "2호선", "역11"),
                new SubwayStationResponseDto(12L, "수도권", "2호선", "역12")
        );
        SubwayRouteResponseDto route = new SubwayRouteResponseDto(11, 0, stations);

        given(subwayService.findShortestRouteByPlaceIds(101L, null, 420L)).willReturn(route);
        given(deliveryPointProperties.getBase()).willReturn(2000L);
        given(deliveryPointProperties.pointForRoute(11)).willReturn(200L);
        given(deliveryPointProperties.pointForSize("L")).willReturn(1000L);

        // When
        SenderPaymentAmountDto result = senderQueryService.getPaymentAmount(101L, 420L, "L");

        // Then
        assertThat(result.getBasePoint()).isEqualTo(2000L);
        assertThat(result.getDistancePoint()).isEqualTo(200L);
        assertThat(result.getWeightPoint()).isEqualTo(1000L);
        assertThat(result.getTotalPoint()).isEqualTo(3200L);
    }

    @Test
    @DisplayName("유효하지 않은 장소 ID 지정 시 DeliveryException이 발생한다")
    void getPaymentAmount_invalidPlace() {
        // Given
        given(subwayService.findShortestRouteByPlaceIds(999L, null, 420L)).willThrow(new IllegalArgumentException());

        // When & Then
        assertThatThrownBy(() -> senderQueryService.getPaymentAmount(999L, 420L, "M"))
                .isInstanceOf(DeliveryException.class);
    }

    @Test
    @DisplayName("출발역과 도착역이 동일한 경우 DeliveryException이 발생한다")
    void getPaymentAmount_sameOriginAndDestination() {
        // When & Then
        assertThatThrownBy(() -> senderQueryService.getPaymentAmount(100L, 100L, "S"))
                .isInstanceOf(DeliveryException.class)
                .extracting("code")
                .isEqualTo(DeliveryErrorCode.SAME_ORIGIN_DESTINATION_NOT_ALLOWED);
    }
}
