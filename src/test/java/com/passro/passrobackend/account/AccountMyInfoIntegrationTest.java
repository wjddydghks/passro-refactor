package com.passro.passrobackend.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.passro.passrobackend.account.entity.Account;
import com.passro.passrobackend.account.entity.AccountPlace;
import com.passro.passrobackend.account.entity.WayPoint;
import com.passro.passrobackend.account.repository.AccountPlaceRepository;
import com.passro.passrobackend.account.repository.WayPointRepository;
import com.passro.passrobackend.place.entity.Place;
import com.passro.passrobackend.place.repository.PlaceRepository;
import com.passro.passrobackend.support.IntegrationTestSupport;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class AccountMyInfoIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private AccountPlaceRepository accountPlaceRepository;

    @Autowired
    private WayPointRepository wayPointRepository;

    @Autowired
    private PlaceRepository placeRepository;

    private Place startPlace;
    private Place destinationPlace;
    private Place wayPointPlace;

    @BeforeEach
    void setUpPlaces() {
        startPlace = placeRepository.save(Place.builder()
                .subwayRouteName("2호선")
                .subwayStationName("강남")
                .build());
        destinationPlace = placeRepository.save(Place.builder()
                .subwayRouteName("2호선")
                .subwayStationName("역삼")
                .build());
        wayPointPlace = placeRepository.save(Place.builder()
                .subwayRouteName("2호선")
                .subwayStationName("선릉")
                .build());
    }

    private Account createAccountWithRoute(String prefix) {
        Account account = createAccount(prefix);
        accountPlaceRepository.saveAndFlush(AccountPlace.builder()
                .account(account)
                .startPlace(startPlace)
                .destinationPlace(destinationPlace)
                .build());
        return account;
    }

    @Test
    void shipperMyPageReturnsSavedWayPointsForProfileEdit() throws Exception {
        Account account = createAccountWithRoute("profile-waypoint");
        AccountPlace accountPlace = accountPlaceRepository.findByAccount(account).orElseThrow();
        wayPointRepository.saveAndFlush(WayPoint.builder()
                .accountPlace(accountPlace)
                .place(wayPointPlace)
                .visitOrder(0)
                .build());

        mockMvc.perform(get("/mypage/shipper")
                        .header("Authorization", bearer(accessToken(account))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.startPlace.id").value(startPlace.getId()))
                .andExpect(jsonPath("$.result.destinationPlace.id").value(destinationPlace.getId()))
                .andExpect(jsonPath("$.result.wayPoints[0].id").value(wayPointPlace.getId()))
                .andExpect(jsonPath("$.result.wayPoints[0].routeName").value("2호선"))
                .andExpect(jsonPath("$.result.wayPoints[0].stationName").value("선릉"));
    }

    @Test
    void editMyInfoUpdatesNameBirthNicknamePhoneAndRoute() throws Exception {
        Account account = createAccountWithRoute("edit-info");
        String token = accessToken(account);

        String newNickname = "edited" + UUID.randomUUID().toString().substring(0, 6);
        String newName = "수정된이름";
        LocalDate newBirth = LocalDate.of(1999, 12, 31);
        String newPhoneNumber = "010-9999-8888";

        String requestBody = """
                {
                  "nickname":"%s",
                  "name":"%s",
                  "birth":"%s",
                  "phoneNumber":"%s",
                  "startPlaceId":%d,
                  "destinationPlaceId":%d,
                  "wayPoints":[%d]
                }
                """.formatted(newNickname, newName, newBirth, newPhoneNumber,
                        startPlace.getId(), destinationPlace.getId(), wayPointPlace.getId());

        mockMvc.perform(patch("/mypage/edit/myInfo")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("ACCOUNT200_1"));

        Account updated = accountRepository.findById(account.getId()).orElseThrow();
        assertThat(updated.getNickname()).isEqualTo(newNickname);
        assertThat(updated.getName()).isEqualTo(newName);
        assertThat(updated.getBirth()).isEqualTo(newBirth);
        assertThat(updated.getPhoneNumber()).isEqualTo(newPhoneNumber);

        AccountPlace updatedPlace = accountPlaceRepository.findByAccount(updated).orElseThrow();
        assertThat(updatedPlace.getStartPlace().getId()).isEqualTo(startPlace.getId());
        assertThat(updatedPlace.getDestinationPlace().getId()).isEqualTo(destinationPlace.getId());
    }

    @Test
    void editMyInfoRejectsDuplicateNickname() throws Exception {
        Account other = createAccountWithRoute("other-nick");
        Account account = createAccountWithRoute("edit-nick");
        String token = accessToken(account);

        String requestBody = """
                {
                  "nickname":"%s",
                  "name":"%s",
                  "birth":"%s",
                  "phoneNumber":"%s",
                  "startPlaceId":%d,
                  "destinationPlaceId":%d
                }
                """.formatted(other.getNickname(), account.getName(), account.getBirth(),
                        account.getPhoneNumber(), startPlace.getId(), destinationPlace.getId());

        mockMvc.perform(patch("/mypage/edit/myInfo")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ACCOUNT400_6"));
    }

    @Test
    void editMyInfoRejectsDuplicatePhoneNumber() throws Exception {
        Account other = createAccountWithRoute("other-phone");
        Account account = createAccountWithRoute("edit-phone");
        String token = accessToken(account);

        String requestBody = """
                {
                  "nickname":"%s",
                  "name":"%s",
                  "birth":"%s",
                  "phoneNumber":"%s",
                  "startPlaceId":%d,
                  "destinationPlaceId":%d
                }
                """.formatted(account.getNickname(), account.getName(), account.getBirth(),
                        other.getPhoneNumber(), startPlace.getId(), destinationPlace.getId());

        mockMvc.perform(patch("/mypage/edit/myInfo")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
        // NOTE: DUPLICATE_NICKNAME과 코드값이 겹쳐서(ACCOUNT400_5) 정확한 코드 구분이 안 됨.
        // AccountErrorCode.DUPLICATE_PHONE_NUMBER 코드값 분리 후 아래 라인 추가 권장
        // .andExpect(jsonPath("$.code").value("ACCOUNT400_8"));
    }

    @Test
    void editMyInfoRejectsInvalidPlaceId() throws Exception {
        Account account = createAccountWithRoute("edit-place");
        String token = accessToken(account);

        String requestBody = """
                {
                  "nickname":"%s",
                  "name":"%s",
                  "birth":"%s",
                  "phoneNumber":"%s",
                  "startPlaceId":999999,
                  "destinationPlaceId":%d
                }
                """.formatted(account.getNickname(), account.getName(), account.getBirth(),
                        account.getPhoneNumber(), destinationPlace.getId());

        mockMvc.perform(patch("/mypage/edit/myInfo")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Account404_2"));
    }

    @Test
    void editMyInfoRejectsInvalidNicknameFormat() throws Exception {
        Account account = createAccountWithRoute("edit-format");
        String token = accessToken(account);

        String requestBody = """
                {
                  "nickname":"ㄴㅇㅁㄴㅇㄴ",
                  "name":"%s",
                  "birth":"%s",
                  "phoneNumber":"%s",
                  "startPlaceId":%d,
                  "destinationPlaceId":%d
                }
                """.formatted(account.getName(), account.getBirth(), account.getPhoneNumber(),
                        startPlace.getId(), destinationPlace.getId());

        mockMvc.perform(patch("/mypage/edit/myInfo")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    void editMyInfoIsRateLimitedByCooldown() throws Exception {
        Account account = createAccountWithRoute("edit-cooldown");
        String token = accessToken(account);

        String requestBody = """
                {
                  "nickname":"%s",
                  "name":"%s",
                  "birth":"%s",
                  "phoneNumber":"%s",
                  "startPlaceId":%d,
                  "destinationPlaceId":%d
                }
                """.formatted(account.getNickname(), account.getName(), account.getBirth(),
                        account.getPhoneNumber(), startPlace.getId(), destinationPlace.getId());

        mockMvc.perform(patch("/mypage/edit/myInfo")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/mypage/edit/myInfo")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("ACCOUNT429_1"));
    }

    @Test
    void editMyInfoRejectsAnonymousRequest() throws Exception {
        mockMvc.perform(patch("/mypage/edit/myInfo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }
}
