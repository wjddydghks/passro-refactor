package com.passro.passrobackend.delivery.location.service;

import com.passro.passrobackend.account.entity.Account;
import com.passro.passrobackend.delivery.entity.Delivery;
import com.passro.passrobackend.delivery.enums.DeliveryState;
import com.passro.passrobackend.delivery.exception.DeliveryException;
import com.passro.passrobackend.delivery.exception.code.DeliveryErrorCode;
import com.passro.passrobackend.delivery.location.dto.ShipperLocationResponseDto;
import com.passro.passrobackend.delivery.location.dto.ShipperLocationUpdateRequestDto;
import com.passro.passrobackend.delivery.location.exception.ShipperLocationException;
import com.passro.passrobackend.delivery.location.exception.code.ShipperLocationErrorCode;
import com.passro.passrobackend.delivery.location.model.ShipperLocation;
import com.passro.passrobackend.delivery.location.repository.ShipperLocationRepository;
import com.passro.passrobackend.delivery.repository.DeliveryRepository;
import com.passro.passrobackend.place.entity.Place;
import com.passro.passrobackend.place.repository.PlaceRepository;
import com.passro.passrobackend.sender.service.SenderDeliveryValidator;
import com.passro.passrobackend.subway.code.SubwayErrorCode;
import com.passro.passrobackend.subway.dto.SubwayRouteResponseDto;
import com.passro.passrobackend.subway.service.SubwayService;
import com.passro.passrobackend.subway.service.SubwayTravelTimeCalculator;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ShipperLocationService {

    private final DeliveryRepository deliveryRepository;
    private final PlaceRepository placeRepository;
    private final SenderDeliveryValidator senderDeliveryValidator;
    private final ShipperLocationRepository shipperLocationRepository;
    private final SubwayService subwayService;

    public ShipperLocationResponseDto updateLocation(
            Account shipper,
            ShipperLocationUpdateRequestDto request) {
        if (!deliveryRepository.existsByShipperAndStatus(shipper, DeliveryState.DELIVERING)) {
            throw new ShipperLocationException(ShipperLocationErrorCode.UPDATE_NOT_ALLOWED);
        }
        Long placeId = resolvePlaceId(request);

        ShipperLocation location = new ShipperLocation(
                request.latitude(),
                request.longitude(),
                placeId,
                LocalDateTime.now());
        shipperLocationRepository.save(shipper.getId(), location);
        return ShipperLocationResponseDto.from(location);
    }

    private Long resolvePlaceId(ShipperLocationUpdateRequestDto request) {
        if (request.placeId() != null) {
            if (!placeRepository.existsById(request.placeId())) {
                throw new DeliveryException(DeliveryErrorCode.PLACE_NOT_FOUND);
            }
            return request.placeId();
        }

        return placeRepository.findAllByLatitudeIsNotNullAndLongitudeIsNotNull().stream()
                .min(Comparator
                        .comparingDouble((Place place) -> distanceScore(
                                request.latitude(), request.longitude(), place))
                        .thenComparing(Place::getId))
                .map(Place::getId)
                .orElseThrow(() -> new DeliveryException(DeliveryErrorCode.PLACE_NOT_FOUND));
    }

    private double distanceScore(BigDecimal latitude, BigDecimal longitude, Place place) {
        double sourceLatitude = Math.toRadians(latitude.doubleValue());
        double targetLatitude = Math.toRadians(place.getLatitude().doubleValue());
        double latitudeDelta = targetLatitude - sourceLatitude;
        double longitudeDelta = Math.toRadians(
                place.getLongitude().subtract(longitude).doubleValue());

        double latitudeComponent = Math.sin(latitudeDelta / 2);
        double longitudeComponent = Math.sin(longitudeDelta / 2);
        return latitudeComponent * latitudeComponent
                + Math.cos(sourceLatitude) * Math.cos(targetLatitude)
                * longitudeComponent * longitudeComponent;
    }

    public ShipperLocationResponseDto getLocation(Account sender, Long deliveryId) {
        Delivery delivery = senderDeliveryValidator.getDeliveryAndValidateOwnership(deliveryId, sender);
        if (delivery.getStatus() != DeliveryState.DELIVERING || delivery.getShipper() == null) {
            throw new ShipperLocationException(ShipperLocationErrorCode.TRACKING_NOT_AVAILABLE);
        }

        ShipperLocation location = shipperLocationRepository.findByShipperId(delivery.getShipper().getId())
                .orElseThrow(() -> new ShipperLocationException(ShipperLocationErrorCode.NOT_FOUND));
        int estimatedTimeMinutes = estimateTimeToDestination(location, delivery);
        return ShipperLocationResponseDto.from(location, estimatedTimeMinutes);
    }

    private int estimateTimeToDestination(ShipperLocation location, Delivery delivery) {
        if (delivery.getDest() == null) {
            throw new DeliveryException(DeliveryErrorCode.PLACE_NOT_FOUND);
        }

        try {
            SubwayRouteResponseDto route = subwayService.findShortestRouteByPlaceIds(
                    location.placeId(), null, delivery.getDest().getId());
            return SubwayTravelTimeCalculator.toEstimatedTimeMinutes(route.getShortestDistance());
        } catch (IllegalArgumentException exception) {
            throw new DeliveryException(SubwayErrorCode.PLACE_NOT_FOUND);
        } catch (IllegalStateException exception) {
            throw new DeliveryException(SubwayErrorCode.ROUTE_NOT_FOUND);
        }
    }
}
