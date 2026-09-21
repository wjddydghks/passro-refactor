package com.passro.passrobackend.sender.dto;

import com.passro.passrobackend.delivery.enums.DeliveryState;
import com.passro.passrobackend.place.entity.Place;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SenderDeliveryListDto {
    private Long deliveryId;
    private String name;
    private Place originPlace;
    private Place destPlace;
    private DeliveryState status;
    private LocalDateTime createdAt;
}
