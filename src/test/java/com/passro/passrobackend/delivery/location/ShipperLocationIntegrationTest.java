package com.passro.passrobackend.delivery.location;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.passro.passrobackend.account.entity.Account;
import com.passro.passrobackend.delivery.entity.Delivery;
import com.passro.passrobackend.delivery.enums.DeliveryState;
import com.passro.passrobackend.place.entity.Place;
import com.passro.passrobackend.place.repository.PlaceRepository;
import com.passro.passrobackend.support.IntegrationTestSupport;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class ShipperLocationIntegrationTest extends IntegrationTestSupport {

    private static final String LOCATION_KEY_PREFIX = "shipper:location:";

    @Autowired
    private PlaceRepository placeRepository;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private final List<Long> shipperIdsToClean = new ArrayList<>();

    @AfterEach
    void cleanLocations() {
        shipperIdsToClean.forEach(id -> redisTemplate.delete(LOCATION_KEY_PREFIX + id));
    }

    @Test
    void deliveringShipperCanUpdateAndOwnerSenderCanReadLocation() throws Exception {
        Place place = requiredPlace("공항", "서울역");
        Account sender = createAccount("location-sender");
        Account shipper = createAccount("location-shipper");
        shipperIdsToClean.add(shipper.getId());
        Delivery delivery = createAssignedDelivery(sender, shipper, DeliveryState.DELIVERING);

        mockMvc.perform(put("/shipper/location")
                        .header("Authorization", bearer(accessToken(shipper)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(locationBody("37.497942", "127.027621", place.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.latitude").value(37.497942))
                .andExpect(jsonPath("$.result.longitude").value(127.027621))
                .andExpect(jsonPath("$.result.placeId").value(place.getId()))
                .andExpect(jsonPath("$.result.updatedAt").isNotEmpty())
                .andExpect(jsonPath("$.result.estimatedTimeMinutes").isEmpty());

        Long ttlSeconds = redisTemplate.getExpire(
                LOCATION_KEY_PREFIX + shipper.getId(), TimeUnit.SECONDS);
        org.assertj.core.api.Assertions.assertThat(ttlSeconds)
                .isBetween(1L, 120L);

        mockMvc.perform(get("/sender/{deliveryId}/shipper-location", delivery.getId())
                        .header("Authorization", bearer(accessToken(sender))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.latitude").value(37.497942))
                .andExpect(jsonPath("$.result.longitude").value(127.027621))
                .andExpect(jsonPath("$.result.placeId").value(place.getId()))
                .andExpect(jsonPath("$.result.updatedAt").isNotEmpty())
                .andExpect(jsonPath("$.result.estimatedTimeMinutes").value(3));
    }

    @Test
    void missingPlaceIdSelectsNearestPlaceFromCoordinates() throws Exception {
        Place nearestPlace = createPlace("자동선택최근접역", "37.51000000", "127.02000000");
        createPlace("자동선택원거리역", "37.61000000", "127.12000000");
        Account sender = createAccount("location-nearest-sender");
        Account shipper = createAccount("location-nearest-shipper");
        shipperIdsToClean.add(shipper.getId());
        createAssignedDelivery(sender, shipper, DeliveryState.DELIVERING);

        mockMvc.perform(put("/shipper/location")
                        .header("Authorization", bearer(accessToken(shipper)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(locationBodyWithoutPlaceId("37.51000001", "127.02000001")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.latitude").value(37.51000001))
                .andExpect(jsonPath("$.result.longitude").value(127.02000001))
                .andExpect(jsonPath("$.result.placeId").value(nearestPlace.getId()));
    }

    @Test
    void openApiDocumentsAutomaticNearestPlaceSelection() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/shipper/location'].put.description")
                        .value(containsString("placeId는 선택값")))
                .andExpect(jsonPath(
                        "$.components.schemas.ShipperLocationUpdateRequestDto.properties.placeId.description")
                        .value(containsString("가장 가까운 지하철역을 자동 선택")))
                .andExpect(jsonPath(
                        "$.components.schemas.ShipperLocationResponse.properties.estimatedTimeMinutes.description")
                        .value(containsString("예상 소요시간")))
                .andExpect(jsonPath("$.components.schemas.ShipperLocationResponse.type")
                        .value("object"))
                .andExpect(jsonPath(
                        "$.components.schemas.APIResponseShipperLocationResponse.properties.result['$ref']")
                        .value("#/components/schemas/ShipperLocationResponse"));
    }

    @Test
    void anotherSenderCannotReadShipperLocation() throws Exception {
        Place place = createPlace("권한테스트역");
        Account owner = createAccount("location-owner");
        Account stranger = createAccount("location-stranger");
        Account shipper = createAccount("location-owner-shipper");
        shipperIdsToClean.add(shipper.getId());
        Delivery delivery = createAssignedDelivery(owner, shipper, DeliveryState.DELIVERING);
        updateLocation(shipper, place.getId());

        mockMvc.perform(get("/sender/{deliveryId}/shipper-location", delivery.getId())
                        .header("Authorization", bearer(accessToken(stranger))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("DELIVERY403_1"));
    }

    @Test
    void locationCanOnlyBeReadWhileDeliveryIsDelivering() throws Exception {
        Place place = createPlace("상태테스트역");
        Account sender = createAccount("location-state-sender");
        Account shipper = createAccount("location-state-shipper");
        shipperIdsToClean.add(shipper.getId());
        Delivery delivery = createAssignedDelivery(sender, shipper, DeliveryState.DELIVERING);
        updateLocation(shipper, place.getId());
        delivery.setStatus(DeliveryState.CONFIRM_REQUESTED);
        deliveryRepository.saveAndFlush(delivery);

        mockMvc.perform(get("/sender/{deliveryId}/shipper-location", delivery.getId())
                        .header("Authorization", bearer(accessToken(sender))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("SHIPPER_LOCATION400_1"));
    }

    @Test
    void accountWithoutDeliveringAssignmentCannotUpdateLocation() throws Exception {
        Place place = createPlace("갱신권한테스트역");
        Account account = createAccount("location-no-delivery");

        mockMvc.perform(put("/shipper/location")
                        .header("Authorization", bearer(accessToken(account)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(locationBody("37.5", "127.0", place.getId())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("SHIPPER_LOCATION403_1"));
    }

    @Test
    void invalidCoordinatesAndUnknownPlaceAreRejected() throws Exception {
        Place place = createPlace("검증테스트역");
        Account sender = createAccount("location-validation-sender");
        Account shipper = createAccount("location-validation-shipper");
        createAssignedDelivery(sender, shipper, DeliveryState.DELIVERING);

        mockMvc.perform(put("/shipper/location")
                        .header("Authorization", bearer(accessToken(shipper)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(locationBody("91", "127", place.getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON400"));

        mockMvc.perform(put("/shipper/location")
                        .header("Authorization", bearer(accessToken(shipper)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(locationBody("37.5", "127", Long.MAX_VALUE)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("DELIVERY404_2"));
    }

    @Test
    void missingLocationReturnsNotFound() throws Exception {
        Account sender = createAccount("location-empty-sender");
        Account shipper = createAccount("location-empty-shipper");
        Delivery delivery = createAssignedDelivery(sender, shipper, DeliveryState.DELIVERING);

        mockMvc.perform(get("/sender/{deliveryId}/shipper-location", delivery.getId())
                        .header("Authorization", bearer(accessToken(sender))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SHIPPER_LOCATION404_1"));
    }

    private Place createPlace(String stationName) {
        return placeRepository.saveAndFlush(Place.builder()
                .subwayRouteName("1호선")
                .subwayStationName(stationName)
                .build());
    }

    private Place createPlace(
            String stationName,
            String latitude,
            String longitude) {
        return placeRepository.saveAndFlush(Place.builder()
                .subwayRouteName("1호선")
                .subwayStationName(stationName)
                .latitude(new BigDecimal(latitude))
                .longitude(new BigDecimal(longitude))
                .build());
    }

    private Delivery createAssignedDelivery(
            Account sender,
            Account shipper,
            DeliveryState status) {
        Place origin = requiredPlace("공항", "서울역");
        Place destination = requiredPlace("공항", "공덕");
        return deliveryRepository.saveAndFlush(Delivery.builder()
                .sender(sender)
                .shipper(shipper)
                .origin(origin)
                .dest(destination)
                .status(status)
                .build());
    }

    private Place requiredPlace(String routeName, String stationName) {
        return placeRepository.findBySubwayRouteNameAndSubwayStationName(routeName, stationName)
                .orElseThrow();
    }

    private void updateLocation(Account shipper, Long placeId) throws Exception {
        mockMvc.perform(put("/shipper/location")
                        .header("Authorization", bearer(accessToken(shipper)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(locationBody("37.5", "127.0", placeId)))
                .andExpect(status().isOk());
    }

    private String locationBody(String latitude, String longitude, Long placeId) {
        return """
                {
                  "latitude": %s,
                  "longitude": %s,
                  "placeId": %d
                }
                """.formatted(latitude, longitude, placeId);
    }

    private String locationBodyWithoutPlaceId(String latitude, String longitude) {
        return """
                {
                  "latitude": %s,
                  "longitude": %s
                }
                """.formatted(latitude, longitude);
    }
}
