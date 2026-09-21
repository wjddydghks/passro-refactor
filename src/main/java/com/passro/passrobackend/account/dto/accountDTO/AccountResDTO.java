package com.passro.passrobackend.account.dto.accountDTO;

import com.passro.passrobackend.subway.dto.SubwayStationResponseDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class AccountResDTO {

    @Getter
    @AllArgsConstructor
    @Schema(types = "object", description = "배송기사 마이페이지 응답")
    public static class ShipperMyPage{
        private String picture;
        private String nickname;
        private String name;
        private LocalDate birth;
        private String phoneNumber;
        private SubwayStationResponseDto startPlace;
        private SubwayStationResponseDto destinationPlace;
        private List<SubwayStationResponseDto> wayPoints;
        private Long deliveryCount;
        private Long point;
        private LocalDateTime createdAt;
        private double rating;
    }

    @Getter
    @AllArgsConstructor
    @Schema(types = "object", description = "발송자 마이페이지 응답")
    public static class SenderMyPage{
        private String picture;
        private String nickname;
        private Long deliveryCount;
        private Long point;
    }
}
