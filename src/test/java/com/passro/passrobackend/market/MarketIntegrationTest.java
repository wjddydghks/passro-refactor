package com.passro.passrobackend.market;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.passro.passrobackend.account.entity.Account;
import com.passro.passrobackend.account.enums.AccountRole;
import com.passro.passrobackend.file.service.S3Service;
import com.passro.passrobackend.market.entity.Market;
import com.passro.passrobackend.market.enums.MarketCategory;
import com.passro.passrobackend.market.repository.MarketRepository;
import com.passro.passrobackend.point.entity.PointLog;
import com.passro.passrobackend.point.enums.PointIncrementReason;
import com.passro.passrobackend.point.repository.PointLogRepository;
import com.passro.passrobackend.support.IntegrationTestSupport;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

class MarketIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private MarketRepository marketRepository;

    @Autowired
    private PointLogRepository pointLogRepository;

    @MockitoBean
    private S3Service s3Service;

    @Test
    void adminCanCreateMarketItem() throws Exception {
        Account admin = createAccount("market-admin");
        admin.setRole(AccountRole.ADMIN);
        accountRepository.saveAndFlush(admin);
        String uploadKey = "uploads/images/123e4567-e89b-12d3-a456-426614174000.png";
        String finalKey = "market-images/123e4567-e89b-12d3-a456-426614174001.png";
        given(s3Service.finalizeUploadedImage(uploadKey, "market-images/")).willReturn(finalKey);

        mockMvc.perform(post("/market")
                        .header("Authorization", bearer(accessToken(admin)))
                        .contentType("application/json")
                        .content("""
                                {
                                  "name": "스타벅스 아메리카노",
                                  "price": 4000,
                                  "category": "카페",
                                  "imageKey": "%s"
                                }
                                """.formatted(uploadKey)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("MARKET201_1"))
                .andExpect(jsonPath("$.result.name").value("스타벅스 아메리카노"))
                .andExpect(jsonPath("$.result.price").value(4000))
                .andExpect(jsonPath("$.result.category").value("카페"))
                .andExpect(jsonPath("$.result.imageKey").value(finalKey));

        verify(s3Service).finalizeUploadedImage(uploadKey, "market-images/");
        assertThat(marketRepository.findAllByCategoryOrderByIdAsc(MarketCategory.CAFE))
                .anySatisfy(market -> {
                    assertThat(market.getName()).isEqualTo("스타벅스 아메리카노");
                    assertThat(market.getImageKey()).isEqualTo(finalKey);
                });
    }

    @Test
    void regularUserCannotCreateMarketItem() throws Exception {
        Account account = createAccount("market-uploader");

        mockMvc.perform(post("/market")
                        .header("Authorization", bearer(accessToken(account)))
                        .contentType("application/json")
                        .content("""
                                {
                                  "name": "등록 불가 상품",
                                  "price": 1000,
                                  "category": "기타",
                                  "imageKey": "uploads/images/123e4567-e89b-12d3-a456-426614174000.png"
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void userCanPurchaseMarketItemWithPoints() throws Exception {
        Account account = createAccount("market-buyer");
        account.setPoint(5000L);
        accountRepository.saveAndFlush(account);
        Market market = saveMarket("커피 쿠폰", 3000L);

        mockMvc.perform(post("/market/{marketId}/purchase", market.getId())
                        .header("Authorization", bearer(accessToken(account))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("MARKET200_1"))
                .andExpect(jsonPath("$.result.item.id").value(market.getId()))
                .andExpect(jsonPath("$.result.item.name").value("커피 쿠폰"))
                .andExpect(jsonPath("$.result.item.category").value("기타"))
                .andExpect(jsonPath("$.result.beforePoint").value(5000))
                .andExpect(jsonPath("$.result.usedPoint").value(3000))
                .andExpect(jsonPath("$.result.remainingPoint").value(2000));

        entityManager.clear();
        Account updatedAccount = accountRepository.findById(account.getId()).orElseThrow();
        assertThat(updatedAccount.currentPoint()).isEqualTo(2000L);

        List<PointLog> logs = pointLogRepository.findAllByAccountOrderByCreatedAtDesc(updatedAccount);
        assertThat(logs).hasSize(1);
        assertThat(logs.getFirst().getIncrementReason()).isEqualTo(PointIncrementReason.MARKET_PURCHASE);
        assertThat(logs.getFirst().getMarket().getId()).isEqualTo(market.getId());
        assertThat(logs.getFirst().getDelivery()).isNull();
        assertThat(logs.getFirst().getDeltaPoint()).isEqualTo(-3000L);
        assertThat(logs.getFirst().getBeforePoint()).isEqualTo(5000L);
        assertThat(logs.getFirst().getAfterPoint()).isEqualTo(2000L);
    }

    @Test
    void purchaseFailsWhenPointsAreInsufficient() throws Exception {
        Account account = createAccount("poor-buyer");
        account.setPoint(1000L);
        accountRepository.saveAndFlush(account);
        Market market = saveMarket("영화 쿠폰", 5000L);

        mockMvc.perform(post("/market/{marketId}/purchase", market.getId())
                        .header("Authorization", bearer(accessToken(account))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("POINT409_1"));

        entityManager.clear();
        Account updatedAccount = accountRepository.findById(account.getId()).orElseThrow();
        assertThat(updatedAccount.currentPoint()).isEqualTo(1000L);
        assertThat(pointLogRepository.findAllByAccountOrderByCreatedAtDesc(updatedAccount)).isEmpty();
    }

    @Test
    void purchaseFailsWhenMarketItemDoesNotExist() throws Exception {
        Account account = createAccount("missing-item-buyer");

        mockMvc.perform(post("/market/{marketId}/purchase", Long.MAX_VALUE)
                        .header("Authorization", bearer(accessToken(account))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("MARKET404_1"));
    }

    @Test
    void userCanReadMarketItems() throws Exception {
        Account account = createAccount("market-reader");
        Market market = saveMarket(
                "편의점 쿠폰",
                2000L,
                MarketCategory.CONVENIENCE_STORE,
                "uploads/images/123e4567-e89b-12d3-a456-426614174000.png");

        mockMvc.perform(get("/market")
                        .header("Authorization", bearer(accessToken(account))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result[?(@.id == %d)].name".formatted(market.getId()))
                        .value(hasItem("편의점 쿠폰")))
                .andExpect(jsonPath("$.result[?(@.id == %d)].category".formatted(market.getId()))
                        .value(hasItem("편의점")))
                .andExpect(jsonPath("$.result[?(@.id == %d)].imageKey".formatted(market.getId()))
                        .value(hasItem("uploads/images/123e4567-e89b-12d3-a456-426614174000.png")));
    }

    @Test
    void userCanFilterMarketItemsByCategory() throws Exception {
        Account account = createAccount("market-filter");
        Market cafe = saveMarket("아메리카노", 4000L, MarketCategory.CAFE);
        saveMarket("치킨 쿠폰", 20000L, MarketCategory.FOOD);

        mockMvc.perform(get("/market")
                        .param("category", "카페")
                        .header("Authorization", bearer(accessToken(account))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result[?(@.id == %d)].name".formatted(cafe.getId()))
                        .value(hasItem("아메리카노")))
                .andExpect(jsonPath("$.result[*].category").value(everyItem(is("카페"))));
    }

    @Test
    void marketFilterRejectsUnsupportedCategory() throws Exception {
        Account account = createAccount("market-invalid-category");

        mockMvc.perform(get("/market")
                        .param("category", "전자제품")
                        .header("Authorization", bearer(accessToken(account))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MARKET400_1"));
    }

    private Market saveMarket(String name, long price) {
        return saveMarket(name, price, MarketCategory.ETC);
    }

    private Market saveMarket(String name, long price, MarketCategory category) {
        return saveMarket(name, price, category, null);
    }

    private Market saveMarket(String name, long price, MarketCategory category, String imageKey) {
        return marketRepository.saveAndFlush(Market.builder()
                .name(name)
                .price(price)
                .category(category)
                .imageKey(imageKey)
                .build());
    }
}
