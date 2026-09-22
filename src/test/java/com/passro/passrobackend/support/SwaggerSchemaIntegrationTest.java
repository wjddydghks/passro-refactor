package com.passro.passrobackend.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class SwaggerSchemaIntegrationTest extends IntegrationTestSupport {

    @Test
    void swaggerTagsFollowUserFlowOrder() throws Exception {
        String body = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode document = objectMapper.readTree(body);

        List<String> tagNames = new ArrayList<>();
        document.at("/tags").forEach(tag -> tagNames.add(tag.path("name").asText()));

        assertThat(tagNames).containsExactly(
                "인증",
                "계정",
                "포인트",
                "마켓",
                "지하철",
                "발송자",
                "전달자",
                "채팅",
                "배송 문의",
                "리뷰",
                "문의(공통)",
                "파일");
    }

    @Test
    void senderDeliveryCreateRequestDoesNotExposePrice() throws Exception {
        String body = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode document = objectMapper.readTree(body);

        JsonNode requestSchema = document.at("/components/schemas/SenderDeliveryCreateRequestDto");
        assertThat(requestSchema.at("/properties/price").isMissingNode()).isTrue();
        assertThat(requestSchema.at("/required").toString()).doesNotContain("\"price\"");
    }

    @Test
    void senderDetailResponseUsesObjectDtoSchema() throws Exception {
        String body = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode document = objectMapper.readTree(body);

        assertThat(document.at(
                "/paths/~1sender~1{deliveryId}/get/responses/200/content/*~1*/schema/$ref").asText())
                .isEqualTo("#/components/schemas/APIResponseSenderDeliveryDetailDto");
        assertThat(document.at(
                "/components/schemas/APIResponseSenderDeliveryDetailDto/properties/result/$ref").asText())
                .isEqualTo("#/components/schemas/SenderDeliveryDetailDto");
        assertThat(document.at(
                "/components/schemas/SenderDeliveryDetailDto/type").asText())
                .isEqualTo("object");
        assertThat(document.at(
                "/components/schemas/SenderDeliveryDetailDto/properties/originPlace/$ref").asText())
                .isEqualTo("#/components/schemas/Place");
        assertThat(document.at(
                "/components/schemas/SenderDeliveryDetailDto/properties/destPlace/$ref").asText())
                .isEqualTo("#/components/schemas/Place");
    }

    @Test
    void shipperDetailResponseUsesCurrentPlaceSchema() throws Exception {
        String body = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode document = objectMapper.readTree(body);

        assertThat(document.at(
                "/paths/~1shipper~1{deliveryId}~1/get/responses/200/content/*~1*/schema/$ref").asText())
                .isEqualTo("#/components/schemas/APIResponseShipperDeliveryDetailDto");
        assertThat(document.at(
                "/components/schemas/APIResponseShipperDeliveryDetailDto/properties/result/$ref").asText())
                .isEqualTo("#/components/schemas/ShipperDeliveryDetailDto");
        assertThat(document.at(
                "/components/schemas/ShipperDeliveryDetailDto/type").asText())
                .isEqualTo("object");
        assertThat(document.at(
                "/components/schemas/Place/properties/subwayRouteName").isMissingNode())
                .isFalse();
        assertThat(document.at(
                "/components/schemas/Place/properties/subwayStationName").isMissingNode())
                .isFalse();
    }

    @Test
    void myPagePointAndChatResponsesUseObjectDtoSchemas() throws Exception {
        String body = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode document = objectMapper.readTree(body);

        assertThat(document.at("/components/schemas/ShipperMyPage/type").asText())
                .isEqualTo("object");
        assertThat(document.at("/components/schemas/SenderMyPage/type").asText())
                .isEqualTo("object");
        assertThat(document.at("/components/schemas/PointHistoryResponseDto/type").asText())
                .isEqualTo("object");
        assertThat(document.at("/components/schemas/PointLogResponseDto/type").asText())
                .isEqualTo("object");
        assertThat(document.at("/components/schemas/PointDeliveryResponseDto/type").asText())
                .isEqualTo("object");
        assertThat(document.at("/components/schemas/PlaceResponseDto/type").asText())
                .isEqualTo("object");
        assertThat(document.at("/components/schemas/ChatRoomInfoResponseDto/type").asText())
                .isEqualTo("object");
        assertThat(document.at("/components/schemas/ChatMessageResponseDto/type").asText())
                .isEqualTo("object");
    }
}
