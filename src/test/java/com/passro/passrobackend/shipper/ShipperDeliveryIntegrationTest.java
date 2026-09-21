package com.passro.passrobackend.shipper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.passro.passrobackend.account.entity.Account;
import com.passro.passrobackend.delivery.entity.Delivery;
import com.passro.passrobackend.delivery.enums.DeliveryLogType;
import com.passro.passrobackend.delivery.enums.DeliveryState;
import com.passro.passrobackend.delivery.exception.DeliveryException;
import com.passro.passrobackend.delivery.exception.code.DeliveryErrorCode;
import com.passro.passrobackend.delivery.repository.DeliveryLogRepository;
import com.passro.passrobackend.place.entity.Place;
import com.passro.passrobackend.place.repository.PlaceRepository;
import com.passro.passrobackend.notification.service.NotificationService;
import com.passro.passrobackend.shipper.service.ShipperService;
import com.passro.passrobackend.support.IntegrationTestSupport;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class ShipperDeliveryIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private DeliveryLogRepository deliveryLogRepository;

    @Autowired
    private ShipperService shipperService;

    @Autowired
    private PlaceRepository placeRepository;

    @Autowired
    private NotificationService notificationService;

    @Test
    void shipperDeliveryDetailContainsCurrentStationFields() throws Exception {
        Account sender = createAccount("station-field-sender");
        Account shipper = createAccount("station-field-shipper");
        Place origin = placeRepository.saveAndFlush(Place.builder()
                .subwayRouteName("2호선")
                .subwayStationName("강남")
                .build());
        Place destination = placeRepository.saveAndFlush(Place.builder()
                .subwayRouteName("신분당선")
                .subwayStationName("판교")
                .build());
        Delivery delivery = deliveryRepository.saveAndFlush(Delivery.builder()
                .sender(sender)
                .shipper(shipper)
                .origin(origin)
                .dest(destination)
                .status(DeliveryState.DELIVERING)
                .build());

        mockMvc.perform(get("/shipper/{deliveryId}/", delivery.getId())
                        .header("Authorization", bearer(accessToken(shipper))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.originPlace.subwayRouteName").value("2호선"))
                .andExpect(jsonPath("$.result.originPlace.subwayStationName").value("강남"))
                .andExpect(jsonPath("$.result.destPlace.subwayRouteName").value("신분당선"))
                .andExpect(jsonPath("$.result.destPlace.subwayStationName").value("판교"));
    }

    @Test
    void shipperCanFilterAssignedDeliveriesByStatusAndReadCreatedAt() throws Exception {
        Account sender = createAccount("filter-shipper-sender");
        Account shipper = createAccount("filter-shipper");
        Delivery matched = deliveryRepository.saveAndFlush(Delivery.builder()
                .sender(sender)
                .shipper(shipper)
                .status(DeliveryState.MATCHED)
                .build());
        Delivery delivering = deliveryRepository.saveAndFlush(Delivery.builder()
                .sender(sender)
                .shipper(shipper)
                .status(DeliveryState.DELIVERING)
                .build());

        mockMvc.perform(get("/shipper/")
                        .param("status", "DELIVERING")
                        .header("Authorization", bearer(accessToken(shipper))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.length()").value(1))
                .andExpect(jsonPath("$.result[0].id").value(delivering.getId()))
                .andExpect(jsonPath("$.result[0].deliveryState").value("DELIVERING"))
                .andExpect(jsonPath("$.result[0].createdAt").isNotEmpty());

        assertThat(matched.getId()).isNotEqualTo(delivering.getId());
    }

    @Test
    void shipperCannotAcceptOwnDeliveryByCallingMatchEndpointDirectly() throws Exception {
        Account account = createAccount("self-delivery");
        Delivery delivery = deliveryRepository.saveAndFlush(Delivery.builder()
                .sender(account)
                .status(DeliveryState.WAIT)
                .terms(true)
                .build());

        mockMvc.perform(patch("/shipper/{deliveryId}/matched", delivery.getId())
                        .header("Authorization", bearer(accessToken(account))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("DELIVERY403_2"));

        entityManager.flush();
        entityManager.clear();
        Delivery persisted = deliveryRepository.findById(delivery.getId()).orElseThrow();
        assertThat(persisted.getStatus()).isEqualTo(DeliveryState.WAIT);
        assertThat(persisted.getShipper()).isNull();
    }

    // @Test // TODO: 출발지/도착지 로직 수정 완료 후 주석 해제
    void shipperCanProgressAssignedDeliveryThroughEveryState() throws Exception {
        Account sender = createAccount("flow-sender");
        Account shipper = createAccount("flow-shipper");
        String senderToken = accessToken(sender);
        String shipperToken = accessToken(shipper);
        long deliveryId = createDelivery(senderToken, "Monitor", 1L, 2L);

        mockMvc.perform(get("/shipper/matched").header("Authorization", bearer(shipperToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result[0].id").value(deliveryId));

        patchAsShipper(deliveryId, "matched", shipperToken);
        assertState(deliveryId, DeliveryState.MATCHED, shipper.getId());

        mockMvc.perform(get("/shipper/").header("Authorization", bearer(shipperToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result[0].id").value(deliveryId));
        mockMvc.perform(get("/shipper/{deliveryId}/", deliveryId)
                        .header("Authorization", bearer(shipperToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.deliveryState").value("MATCHED"));

        patchAsShipper(deliveryId, "acquire", shipperToken);
        assertState(deliveryId, DeliveryState.DELIVERING, shipper.getId());

        patchAsShipper(deliveryId, "confirm", shipperToken);
        assertState(deliveryId, DeliveryState.CONFIRM_REQUESTED, shipper.getId());

        mockMvc.perform(patch("/sender/{deliveryId}/complete", deliveryId)
                        .header("Authorization", bearer(senderToken)))
                .andExpect(status().isOk());
        assertState(deliveryId, DeliveryState.DELIVERED, shipper.getId());

        Delivery delivery = deliveryRepository.findById(deliveryId).orElseThrow();
        assertThat(deliveryLogRepository.findAllByDeliveryOrderByCreatedAtAsc(delivery))
                .extracting(log -> log.getType())
                .containsExactly(
                        DeliveryLogType.SEND_REQUEST,
                        DeliveryLogType.MATCHED,
                        DeliveryLogType.PICKED_UP,
                        DeliveryLogType.DELIVERED,
                        DeliveryLogType.DONE
                );
    }

    // @Test // TODO: 출발지/도착지 로직 수정 완료 후 주석 해제
    void shipperCannotSkipDeliveryStates() throws Exception {
        Account sender = createAccount("state-sender");
        Account shipper = createAccount("state-shipper");
        long deliveryId = createDelivery(accessToken(sender), "Keyboard", 1L, 2L);
        String shipperToken = accessToken(shipper);

        patchAsShipper(deliveryId, "matched", shipperToken);

        mockMvc.perform(patch("/shipper/{deliveryId}/confirm", deliveryId)
                        .header("Authorization", bearer(shipperToken)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("DELIVERY400_3"));

        patchAsShipper(deliveryId, "acquire", shipperToken);
        mockMvc.perform(patch("/shipper/{deliveryId}/matched", deliveryId)
                        .header("Authorization", bearer(shipperToken)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("DELIVERY400_3"));
    }

    // @Test // TODO: 출발지/도착지 로직 수정 완료 후 주석 해제
    void unassignedShipperCannotReadOrProgressAnotherShippersDelivery() throws Exception {
        Account sender = createAccount("owner-sender");
        Account assigned = createAccount("assigned-shipper");
        Account stranger = createAccount("stranger-shipper");
        long deliveryId = createDelivery(accessToken(sender), "Tablet", 1L, 2L);
        patchAsShipper(deliveryId, "matched", accessToken(assigned));
        String strangerToken = accessToken(stranger);

        mockMvc.perform(get("/shipper/{deliveryId}/", deliveryId)
                        .header("Authorization", bearer(strangerToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("DELIVERY403_1"));

        mockMvc.perform(patch("/shipper/{deliveryId}/acquire", deliveryId)
                        .header("Authorization", bearer(strangerToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("DELIVERY403_1"));

        mockMvc.perform(patch("/shipper/{deliveryId}/matched", deliveryId)
                        .header("Authorization", bearer(strangerToken)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("DELIVERY400_3"));
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void onlyOneShipperCanMatchTheSameDeliveryConcurrently() throws Exception {
        Account sender = createAccount("concurrent-sender");
        Account firstShipper = createAccount("concurrent-shipper-a");
        Account secondShipper = createAccount("concurrent-shipper-b");
        Delivery delivery = deliveryRepository.saveAndFlush(Delivery.builder()
                .sender(sender)
                .status(DeliveryState.WAIT)
                .terms(true)
                .build());

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        try {
            Future<Boolean> firstResult = executor.submit(
                    () -> attemptMatch(firstShipper, delivery.getId(), ready, start));
            Future<Boolean> secondResult = executor.submit(
                    () -> attemptMatch(secondShipper, delivery.getId(), ready, start));

            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            assertThat(List.of(
                    firstResult.get(10, TimeUnit.SECONDS),
                    secondResult.get(10, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(true, false);

            Delivery matchedDelivery = deliveryRepository.findById(delivery.getId()).orElseThrow();
            assertThat(matchedDelivery.getStatus()).isEqualTo(DeliveryState.MATCHED);
            assertThat(matchedDelivery.getShipper().getId())
                    .isIn(firstShipper.getId(), secondShipper.getId());
            assertThat(deliveryLogRepository.findAllByDeliveryOrderByCreatedAtAsc(matchedDelivery))
                    .extracting(log -> log.getType())
                    .containsExactly(DeliveryLogType.MATCHED);
        } finally {
            executor.shutdownNow();
            Delivery persistedDelivery = deliveryRepository.findById(delivery.getId()).orElse(null);
            if (persistedDelivery != null) {
                deliveryLogRepository.deleteAll(
                        deliveryLogRepository.findAllByDeliveryOrderByCreatedAtAsc(persistedDelivery));
                deliveryRepository.delete(persistedDelivery);
            }
            notificationService.deleteAllNotifications(sender);
            accountRepository.deleteAllById(
                    List.of(sender.getId(), firstShipper.getId(), secondShipper.getId()));
        }
    }

    private boolean attemptMatch(
            Account shipper,
            Long deliveryId,
            CountDownLatch ready,
            CountDownLatch start) throws InterruptedException {
        ready.countDown();
        start.await();
        try {
            shipperService.matchAccept(shipper, deliveryId);
            return true;
        } catch (DeliveryException exception) {
            assertThat(exception.getCode()).isEqualTo(DeliveryErrorCode.INVALID_STATUS_TRANSITION);
            return false;
        }
    }

    private void patchAsShipper(long deliveryId, String action, String token) throws Exception {
        mockMvc.perform(patch("/shipper/{deliveryId}/{action}", deliveryId, action)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk());
    }

    private void assertState(long deliveryId, DeliveryState expected, Long shipperId) {
        entityManager.flush();
        entityManager.clear();
        Delivery delivery = deliveryRepository.findById(deliveryId).orElseThrow();
        assertThat(delivery.getStatus()).isEqualTo(expected);
        assertThat(delivery.getShipper().getId()).isEqualTo(shipperId);
    }
}
