package com.passro.passrobackend.sender.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.passro.passrobackend.account.entity.Account;
import com.passro.passrobackend.delivery.configuration.DeliveryPointProperties;
import com.passro.passrobackend.delivery.entity.Delivery;
import com.passro.passrobackend.delivery.entity.DeliveryGoodInfo;
import com.passro.passrobackend.delivery.entity.DeliveryPoint;
import com.passro.passrobackend.delivery.enums.DeliveryState;
import com.passro.passrobackend.delivery.enums.DeliveryLogType;
import com.passro.passrobackend.delivery.event.DeliveryLogEvent;
import com.passro.passrobackend.delivery.exception.DeliveryException;
import com.passro.passrobackend.delivery.exception.code.DeliveryErrorCode;
import com.passro.passrobackend.delivery.repository.DeliveryRepository;
import com.passro.passrobackend.place.entity.Place;
import com.passro.passrobackend.place.repository.PlaceRepository;
import com.passro.passrobackend.point.service.PointService;
import com.passro.passrobackend.sender.dto.SenderDeliveryCreateRequestDto;
import com.passro.passrobackend.subway.dto.SubwayRouteResponseDto;
import com.passro.passrobackend.subway.dto.SubwayStationResponseDto;
import com.passro.passrobackend.subway.service.SubwayService;
import com.passro.passrobackend.file.service.S3Service;
import com.passro.passrobackend.notification.enums.NotificationType;
import com.passro.passrobackend.notification.enums.ResourceType;
import com.passro.passrobackend.notification.service.NotificationService;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class SenderCommandServiceTest {

    @Mock
    private DeliveryRepository deliveryRepository;

    @Mock
    private PlaceRepository placeRepository;

    @Mock
    private SenderDeliveryValidator senderDeliveryValidator;

    @Mock
    private SubwayService subwayService;

    @Mock
    private PointService pointService;

    @Mock
    private DeliveryPointProperties deliveryPointProperties;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private S3Service s3Service;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private SenderCommandService senderCommandService;

    @Test
    @DisplayName("출발역과 도착역이 정상적일 때 배송 요청 생성이 성공한다")
    void createDelivery_success() {
        // Given
        Account sender = Account.builder().id(1L).build();
        SenderDeliveryCreateRequestDto request = SenderDeliveryCreateRequestDto.builder()
                .sourceStationId(10L)
                .destinationStationId(20L)
                .name("노트북")
                .size("M")
                .picture("pic.jpg")
                .memo("조심히 배송해 주세요")
                .build();

        Place origin = Place.builder().id(10L).subwayRouteName("2호선").subwayStationName("강남").build();
        Place dest = Place.builder().id(20L).subwayRouteName("2호선").subwayStationName("홍대입구").build();

        given(placeRepository.findById(10L)).willReturn(Optional.of(origin));
        given(placeRepository.findById(20L)).willReturn(Optional.of(dest));
        given(subwayService.getRegionByPlaceId(10L)).willReturn("수도권");
        given(subwayService.getRegionByPlaceId(20L)).willReturn("수도권");
        given(subwayService.findShortestRoute(origin, List.of(), dest))
                .willReturn(new SubwayRouteResponseDto(11, 0,
                        IntStream.rangeClosed(0, 11)
                                .mapToObj(index -> new SubwayStationResponseDto())
                                .toList()));
        given(deliveryPointProperties.getBase()).willReturn(2000L);
        given(deliveryPointProperties.pointForRoute(11)).willReturn(200L);
        given(deliveryPointProperties.pointForSize("M")).willReturn(500L);

        given(deliveryRepository.save(any(Delivery.class))).willAnswer(invocation -> {
            Delivery delivery = invocation.getArgument(0);
            delivery.setId(100L);
            return delivery;
        });

        // When
        Long deliveryId = senderCommandService.createDelivery(sender, request);

        // Then
        assertThat(deliveryId).isEqualTo(100L);
        ArgumentCaptor<Delivery> deliveryCaptor = ArgumentCaptor.forClass(Delivery.class);
        verify(deliveryRepository).save(deliveryCaptor.capture());
        assertThat(deliveryCaptor.getValue().getDeliveryGoodInfo().getName()).isEqualTo("노트북");
        assertThat(deliveryCaptor.getValue().getDeliveryGoodInfo().getPrice()).isEqualTo(2700L);
        assertThat(deliveryCaptor.getValue().getDeliveryPoint().getBase_point()).isEqualTo(2000L);
        assertThat(deliveryCaptor.getValue().getDeliveryPoint().getDistance_point()).isEqualTo(200L);
        assertThat(deliveryCaptor.getValue().getDeliveryPoint().getWeight_point()).isEqualTo(500L);
    }

    @Test
    @DisplayName("출발역과 도착역이 동일하면 SAME_ORIGIN_DESTINATION_NOT_ALLOWED 예외가 발생한다")
    void createDelivery_sameOriginAndDest_throwsException() {
        // Given
        Account sender = Account.builder().id(1L).build();
        SenderDeliveryCreateRequestDto request = SenderDeliveryCreateRequestDto.builder()
                .sourceStationId(10L)
                .destinationStationId(10L)
                .build();

        // When & Then
        assertThatThrownBy(() -> senderCommandService.createDelivery(sender, request))
                .isInstanceOf(DeliveryException.class)
                .extracting(e -> ((DeliveryException) e).getCode())
                .isEqualTo(DeliveryErrorCode.SAME_ORIGIN_DESTINATION_NOT_ALLOWED);
    }

    @Test
    @DisplayName("지하철 데이터베이스에 등록되지 않은 역이면 PLACE_NOT_FOUND 예외가 발생한다")
    void createDelivery_invalidSubwayStation_throwsException() {
        // Given
        Account sender = Account.builder().id(1L).build();
        SenderDeliveryCreateRequestDto request = SenderDeliveryCreateRequestDto.builder()
                .sourceStationId(10L)
                .destinationStationId(20L)
                .build();

        Place origin = Place.builder().id(10L).build();
        Place dest = Place.builder().id(20L).build();

        given(placeRepository.findById(10L)).willReturn(Optional.of(origin));
        given(placeRepository.findById(20L)).willReturn(Optional.of(dest));
        given(subwayService.getRegionByPlaceId(10L)).willReturn(null); // 지하철 그래프에 없음

        // When & Then
        assertThatThrownBy(() -> senderCommandService.createDelivery(sender, request))
                .isInstanceOf(DeliveryException.class)
                .extracting(e -> ((DeliveryException) e).getCode())
                .isEqualTo(DeliveryErrorCode.PLACE_NOT_FOUND);
    }

    @Test
    @DisplayName("검수 요청(CONFIRM_REQUESTED) 상태에서 배송 완료 승인이 성공한다")
    void completeDelivery_success() {
        // Given
        Account sender = Account.builder().id(1L).build();
        Account shipper = Account.builder().id(2L).build();
        Delivery delivery = Delivery.builder()
                .id(100L)
                .sender(sender)
                .shipper(shipper)
                .status(DeliveryState.CONFIRM_REQUESTED)
                .build();
        DeliveryPoint point = point(delivery);

        given(senderDeliveryValidator.getDeliveryForUpdateAndValidateOwnership(100L, sender)).willReturn(delivery);

        // When
        senderCommandService.completeDelivery(sender, 100L);

        // Then
        assertThat(delivery.getStatus()).isEqualTo(DeliveryState.DELIVERED);
        verify(pointService).settleDelivery(2L, delivery, 1700L);
        verify(notificationService).publish(
                shipper,
                NotificationType.DELIVERY,
                "배송 완료",
                "발송자가 배송 완료를 승인했습니다.",
                ResourceType.DELIVERY,
                100L);
    }

    @Test
    @DisplayName("배송 완료 승인 시 임시 이미지를 확정하고 최종 키를 로그 이벤트에 저장한다")
    void completeDelivery_withImage_usesFinalImageKey() {
        Account sender = Account.builder().id(1L).build();
        Account shipper = Account.builder().id(2L).build();
        Delivery delivery = Delivery.builder()
                .id(100L)
                .sender(sender)
                .shipper(shipper)
                .status(DeliveryState.CONFIRM_REQUESTED)
                .build();
        DeliveryPoint point = point(delivery);
        String uploadKey = "uploads/images/123e4567-e89b-12d3-a456-426614174000.jpg";
        String finalKey = "delivery-images/123e4567-e89b-12d3-a456-426614174001.jpg";
        given(senderDeliveryValidator.getDeliveryForUpdateAndValidateOwnership(100L, sender))
                .willReturn(delivery);
        given(s3Service.finalizeUploadedImage(uploadKey, "delivery-images/")).willReturn(finalKey);

        senderCommandService.completeDelivery(sender, 100L, uploadKey);

        ArgumentCaptor<DeliveryLogEvent> eventCaptor = ArgumentCaptor.forClass(DeliveryLogEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        verify(pointService).settleDelivery(2L, delivery, 1700L);
        assertThat(eventCaptor.getValue().getType()).isEqualTo(DeliveryLogType.DONE);
        assertThat(eventCaptor.getValue().getImage()).isEqualTo(finalKey);
    }

    @Test
    @DisplayName("검수 요청 상태가 아닐 때 배송 완료 승인 시 예외가 발생한다")
    void completeDelivery_invalidStatus_throwsException() {
        // Given
        Account sender = Account.builder().id(1L).build();
        Delivery delivery = Delivery.builder()
                .id(100L)
                .sender(sender)
                .status(DeliveryState.WAIT)
                .build();

        given(senderDeliveryValidator.getDeliveryForUpdateAndValidateOwnership(100L, sender)).willReturn(delivery);

        // When & Then
        assertThatThrownBy(() -> senderCommandService.completeDelivery(sender, 100L))
                .isInstanceOf(DeliveryException.class)
                .extracting(e -> ((DeliveryException) e).getCode())
                .isEqualTo(DeliveryErrorCode.INVALID_STATUS_FOR_COMPLETION);
    }

    @Test
    @DisplayName("발송 약관 동의가 성공한다")
    void agreeTerms_success() {
        // Given
        Account sender = Account.builder().id(1L).build();
        Delivery delivery = Delivery.builder()
                .id(100L)
                .sender(sender)
                .status(DeliveryState.WAIT)
                .terms(false)
                .build();
        DeliveryPoint point = point(delivery);

        given(senderDeliveryValidator.getDeliveryForUpdateAndValidateOwnership(100L, sender)).willReturn(delivery);

        // When
        senderCommandService.agreeTerms(sender, 100L);

        // Then
        assertThat(delivery.getTerms()).isTrue();
        verify(pointService).payForDelivery(1L, delivery, 1700L);
    }

    @Test
    @DisplayName("매칭 전(WAIT) 상태에서 발송 요청 취소가 성공한다")
    void cancelDelivery_success() {
        // Given
        Account sender = Account.builder().id(1L).build();
        Delivery delivery = Delivery.builder()
                .id(100L)
                .sender(sender)
                .status(DeliveryState.WAIT)
                .build();
        DeliveryPoint point = point(delivery);

        given(senderDeliveryValidator.getDeliveryForUpdateAndValidateOwnership(100L, sender)).willReturn(delivery);

        // When
        senderCommandService.cancelDelivery(sender, 100L);

        // Then
        assertThat(delivery.getStatus()).isEqualTo(DeliveryState.CANCEL);
        verify(pointService).refundDelivery(1L, delivery, 1700L);
    }

    @Test
    @DisplayName("이미 매칭된(MATCHED) 배송 요청은 취소할 수 없다")
    void cancelDelivery_matched_throwsException() {
        // Given
        Account sender = Account.builder().id(1L).build();
        Delivery delivery = Delivery.builder()
                .id(100L)
                .sender(sender)
                .status(DeliveryState.MATCHED)
                .build();

        given(senderDeliveryValidator.getDeliveryForUpdateAndValidateOwnership(100L, sender)).willReturn(delivery);

        // When & Then
        assertThatThrownBy(() -> senderCommandService.cancelDelivery(sender, 100L))
                .isInstanceOf(DeliveryException.class)
                .extracting(e -> ((DeliveryException) e).getCode())
                .isEqualTo(DeliveryErrorCode.CANNOT_CANCEL);
    }

    private DeliveryPoint point(Delivery delivery) {
        DeliveryPoint point = DeliveryPoint.builder()
                .base_point(1000L)
                .distance_point(500L)
                .weight_point(200L)
                .build();
        delivery.attachPoint(point);
        return point;
    }
}
