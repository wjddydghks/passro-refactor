package com.passro.passrobackend.sender.dto;

import com.passro.passrobackend.delivery.entity.DeliveryPoint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SenderPaymentAmountDto {
    private Long id;
    private Long basePoint;
    private Long distancePoint;
    private Long weightPoint;
    private Long totalPoint;

    public static SenderPaymentAmountDto fromEntity(DeliveryPoint deliveryPoint) {
        if (deliveryPoint == null) {
            return null;
        }
        long base = deliveryPoint.getBase_point() != null ? deliveryPoint.getBase_point() : 0L;
        long distance = deliveryPoint.getDistance_point() != null ? deliveryPoint.getDistance_point() : 0L;
        long weight = deliveryPoint.getWeight_point() != null ? deliveryPoint.getWeight_point() : 0L;
        long total = base + distance + weight;

        return SenderPaymentAmountDto.builder()
                .id(deliveryPoint.getId())
                .basePoint(base)
                .distancePoint(distance)
                .weightPoint(weight)
                .totalPoint(total)
                .build();
    }
}
