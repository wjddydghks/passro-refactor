package com.passro.passrobackend.sender.dto;

import com.passro.passrobackend.account.entity.Account;
import com.passro.passrobackend.account.entity.AccountPlace;
import com.passro.passrobackend.delivery.entity.Delivery;
import com.passro.passrobackend.delivery.entity.DeliveryLog;
import com.passro.passrobackend.delivery.enums.DeliveryLogType;
import com.passro.passrobackend.delivery.enums.DeliveryState;
import com.passro.passrobackend.place.entity.Place;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Function;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(types = "object", description = "발송 배송 상세 응답")
public class SenderDeliveryDetailDto {
    private Long id;
    private String name;
    private Place originPlace;
    private Place destPlace;
    private DeliveryState status; // 현재 배송 상태
    private ShipperInfo shipperInfo; // 배송자 정보
    private List<DeliveryLogInfo> deliveryTimeLine; // 배송 타임라인

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ShipperInfo {
        private String name;
        private String nickname;
        private String picture;
        private Place originPlace;
        private Place destPlace;

        public static ShipperInfo fromAccount(
                Account account,
                AccountPlace accountPlace,
                Function<String, String> imageUrlResolver) {
            if (account == null) {
                return null;
            }
            return ShipperInfo.builder()
                    .name(account.getName())
                    .nickname(account.getNickname())
                    .picture(imageUrlResolver.apply(account.getPicture()))
                    .originPlace(accountPlace != null ? accountPlace.getStartPlace() : null)
                    .destPlace(accountPlace != null ? accountPlace.getDestinationPlace() : null)
                    .build();
        }
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DeliveryLogInfo {
        private Long id;
        private DeliveryLogType type;
        private String image;
        private LocalDateTime createdAt;

        public static DeliveryLogInfo fromEntity(
                DeliveryLog log, Function<String, String> imageUrlResolver) {
            if (log == null) {
                return null;
            }
            return DeliveryLogInfo.builder()
                    .id(log.getId())
                    .type(log.getType())
                    .image(imageUrlResolver.apply(log.getImage()))
                    .createdAt(log.getCreatedAt())
                    .build();
        }
    }

    public static SenderDeliveryDetailDto fromEntity(
            Delivery delivery,
            List<DeliveryLog> logs,
            AccountPlace shipperAccountPlace,
            Function<String, String> imageUrlResolver) {
        return SenderDeliveryDetailDto.builder()
                .id(delivery.getId())
                .name(delivery.getDeliveryGoodInfo() != null
                        ? delivery.getDeliveryGoodInfo().getName()
                        : null)
                .originPlace(delivery.getOrigin())
                .destPlace(delivery.getDest())
                .status(delivery.getStatus())
                .shipperInfo(ShipperInfo.fromAccount(
                        delivery.getShipper(), shipperAccountPlace, imageUrlResolver))
                .deliveryTimeLine(logs != null
                        ? logs.stream().map(log -> DeliveryLogInfo.fromEntity(log, imageUrlResolver)).toList()
                        : List.of())
                .build();
    }
}
