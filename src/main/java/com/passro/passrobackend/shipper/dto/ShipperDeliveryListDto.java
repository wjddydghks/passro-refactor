package com.passro.passrobackend.shipper.dto;

import com.passro.passrobackend.account.entity.Account;
import com.passro.passrobackend.delivery.entity.Delivery;
import com.passro.passrobackend.delivery.enums.DeliveryState;
import com.passro.passrobackend.place.entity.Place;
import java.time.LocalDateTime;
import java.util.function.Function;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
public class ShipperDeliveryListDto {
    private Long id;
    private String name;

    private SenderInfo senderInfo;
    private ShipperInfo shipperInfo;

    private Place originPlace;
    private Place destPlace;

    private DeliveryState deliveryState;
    private String memo;
    private LocalDateTime createdAt;
    private Integer estimatedTimeMinutes;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SenderInfo {
        private String name;
        private String picture;
        private Place place;

        public static SenderInfo fromAccount(Account account, Function<String, String> imageUrlResolver) {
            if (account == null) {
                return null;
            }

            return SenderInfo.builder()
                    .name(account.getName())
                    .picture(imageUrlResolver.apply(account.getPicture()))
                    .place(account.getPlace_id())
                    .build();
        }
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ShipperInfo {
        private String name;
        private String picture;
        private Place place;

        public static ShipperInfo fromAccount(Account account, Function<String, String> imageUrlResolver) {
            if (account == null) {
                return null;
            }

            return ShipperInfo.builder()
                    .name(account.getName())
                    .picture(imageUrlResolver.apply(account.getPicture()))
                    .place(account.getPlace_id())
                    .build();
        }
    }

    public static ShipperDeliveryListDto fromDelivery(
            Delivery delivery,
            Integer estimatedTimeMinutes,
            Function<String, String> imageUrlResolver) {
        return ShipperDeliveryListDto.builder()
                .id(delivery.getId())
                .name(delivery.getDeliveryGoodInfo() != null
                        ? delivery.getDeliveryGoodInfo().getName()
                        : null)
                .senderInfo(SenderInfo.fromAccount(delivery.getSender(), imageUrlResolver))
                .shipperInfo(ShipperInfo.fromAccount(delivery.getShipper(), imageUrlResolver))
                .originPlace(delivery.getOrigin())
                .destPlace(delivery.getDest())
                .memo(delivery.getMemo())
                .deliveryState(delivery.getStatus())
                .createdAt(delivery.getCreatedAt())
                .estimatedTimeMinutes(estimatedTimeMinutes)
                .build();
    }
}
