package com.passro.passrobackend.shipper.dto;

import com.passro.passrobackend.account.entity.Account;
import com.passro.passrobackend.account.entity.AccountPlace;
import com.passro.passrobackend.delivery.entity.Delivery;
import com.passro.passrobackend.delivery.entity.DeliveryGoodInfo;
import com.passro.passrobackend.delivery.entity.DeliveryLog;
import com.passro.passrobackend.delivery.entity.DeliveryPoint;
import com.passro.passrobackend.delivery.enums.DeliveryLogType;
import com.passro.passrobackend.delivery.enums.DeliveryState;
import com.passro.passrobackend.place.entity.Place;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Function;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Schema(types = "object", description = "배송기사 배송 상세 응답")
public class ShipperDeliveryDetailDto {
    private Long id;
    private String name;
    private Long price;
    private String size;

    private Long basePoint;
    private Long distancePoint;
    private Long weightPoint;
    private Long totalPoint;

    private SenderInfo senderInfo;
    private ShipperInfo shipperInfo;

    private Place originPlace;
    private Place destPlace;

    private DeliveryState deliveryState;
    private String memo;

    private List<DeliveryLogInfo> deliveryTimeLine;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SenderInfo {
        private String name;
        private String nickname;
        private String picture;
        private Place originPlace;
        private Place destPlace;

        public static SenderInfo fromAccount(
                Account account,
                AccountPlace accountPlace,
                Function<String, String> imageUrlResolver) {
            if (account == null) {
                return null;
            }

            return SenderInfo.builder()
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

    public static ShipperDeliveryDetailDto fromDelivery(
            Delivery delivery,
            List<DeliveryLog> logs,
            AccountPlace senderAccountPlace,
            AccountPlace shipperAccountPlace,
            Function<String, String> imageUrlResolver) {
        DeliveryGoodInfo goodInfo = delivery.getDeliveryGoodInfo();
        DeliveryPoint point = delivery.getDeliveryPoint();

        Long basePoint = point != null ? pointOrZero(point.getBase_point()) : null;
        Long distancePoint = point != null ? pointOrZero(point.getDistance_point()) : null;
        Long weightPoint = point != null ? pointOrZero(point.getWeight_point()) : null;
        Long totalPoint = point != null
                ? Math.addExact(Math.addExact(basePoint, distancePoint), weightPoint)
                : null;

        return ShipperDeliveryDetailDto.builder()
                .id(delivery.getId())
                .name(goodInfo != null ? goodInfo.getName() : null)
                .price(goodInfo != null ? goodInfo.getPrice() : null)
                .size(goodInfo != null ? goodInfo.getSize() : null)
                .basePoint(basePoint)
                .distancePoint(distancePoint)
                .weightPoint(weightPoint)
                .totalPoint(totalPoint)
                .senderInfo(SenderInfo.fromAccount(
                        delivery.getSender(), senderAccountPlace, imageUrlResolver))
                .shipperInfo(ShipperInfo.fromAccount(
                        delivery.getShipper(), shipperAccountPlace, imageUrlResolver))
                .originPlace(delivery.getOrigin())
                .destPlace(delivery.getDest())
                .memo(delivery.getMemo())
                .deliveryState(delivery.getStatus())
                .deliveryTimeLine(logs != null
                        ? logs.stream().map(log -> DeliveryLogInfo.fromEntity(log, imageUrlResolver)).toList()
                        : List.of())
                .build();
    }

    private static long pointOrZero(Long point) {
        return point != null ? point : 0L;
    }
}
