package com.passro.passrobackend.deliveryinquiry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.passro.passrobackend.account.entity.Account;
import com.passro.passrobackend.delivery.entity.Delivery;
import com.passro.passrobackend.delivery.enums.DeliveryState;
import com.passro.passrobackend.review.repository.ReviewRepository;
import com.passro.passrobackend.support.IntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class DeliveryInquiryReviewIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private ReviewRepository reviewRepository;

    @Test
    void authenticatedUsersCanCreateAndReadDeliveryInquiries() throws Exception {
        Account sender = createAccount("inquiry-sender");
        Account shipper = createAccount("inquiry-shipper");
        Delivery delivery = saveDelivery(sender, shipper, DeliveryState.DELIVERING);

        mockMvc.perform(post("/delivery-inquiry")
                        .header("Authorization", bearer(accessToken(sender)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(deliveryInquiryBody(delivery.getId(), "Where is it?")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.result.deliveryId").value(delivery.getId()))
                .andExpect(jsonPath("$.result.writerNickname").value(sender.getNickname()));

        mockMvc.perform(get("/delivery-inquiry/{deliveryId}", delivery.getId())
                        .header("Authorization", bearer(accessToken(shipper))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result[0].title").value("Delivery question"))
                .andExpect(jsonPath("$.result[0].content").value("Where is it?"));
    }

    @Test
    void inquiryValidationAndMissingDeliveryAreHandled() throws Exception {
        Account account = createAccount("invalid-inquiry");
        String token = accessToken(account);

        mockMvc.perform(post("/delivery-inquiry")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(deliveryInquiryBody(999999L, "")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON400"));

        mockMvc.perform(get("/delivery-inquiry/{deliveryId}", 999999L)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("DELIVERY404_1"));
    }

    @Test
    void senderCanReviewCompletedDeliveryAndAverageIsCalculated() throws Exception {
        Account sender = createAccount("review-sender");
        Account shipper = createAccount("review-shipper");
        Delivery first = saveDelivery(sender, shipper, DeliveryState.DELIVERED);
        Delivery second = saveDelivery(sender, shipper, DeliveryState.DELIVERED);
        String senderToken = accessToken(sender);

        createReview(first.getId(), 5, senderToken).andExpect(status().isCreated());
        createReview(second.getId(), 3, senderToken).andExpect(status().isCreated());

        mockMvc.perform(get("/reviews/average/{userId}", shipper.getId())
                        .header("Authorization", bearer(senderToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.averageRating").value(4.0));
        assertThat(reviewRepository.count()).isEqualTo(2);
    }

    @Test
    void duplicateUnauthorizedAndPrematureReviewsAreRejected() throws Exception {
        Account sender = createAccount("review-owner");
        Account stranger = createAccount("review-stranger");
        Account shipper = createAccount("review-target");
        Delivery delivered = saveDelivery(sender, shipper, DeliveryState.DELIVERED);
        Delivery delivering = saveDelivery(sender, shipper, DeliveryState.DELIVERING);
        String senderToken = accessToken(sender);

        createReview(delivered.getId(), 5, senderToken).andExpect(status().isCreated());
        createReview(delivered.getId(), 4, senderToken)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("REVIEW400_2"));

        createReview(delivered.getId(), 5, accessToken(stranger))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("REVIEW403_1"));

        createReview(delivering.getId(), 5, senderToken)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("REVIEW400_1"));

        createReview(delivered.getId(), 6, senderToken)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("REVIEW400_3"));
    }

    private Delivery saveDelivery(Account sender, Account shipper, DeliveryState state) {
        return deliveryRepository.saveAndFlush(Delivery.builder()
                .sender(sender)
                .shipper(shipper)
                .status(state)
                .terms(true)
                .build());
    }

    private String deliveryInquiryBody(long deliveryId, String content) {
        return """
                {"deliveryId":%d,"category":"ETC","title":"Delivery question","content":"%s"}
                """.formatted(deliveryId, content);
    }

    private org.springframework.test.web.servlet.ResultActions createReview(
            long deliveryId, int rating, String token) throws Exception {
        String body = "{\"deliveryId\":" + deliveryId + ",\"rating\":" + rating
                + ",\"content\":\"Good delivery\"}";
        return mockMvc.perform(post("/reviews")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body));
    }
}
