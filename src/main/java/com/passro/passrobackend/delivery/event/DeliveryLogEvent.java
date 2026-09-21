package com.passro.passrobackend.delivery.event;

import com.passro.passrobackend.delivery.entity.Delivery;
import com.passro.passrobackend.delivery.enums.DeliveryLogType;
import lombok.Getter;

@Getter
public class DeliveryLogEvent {
    private final Delivery delivery;
    private final DeliveryLogType type;
    private final String image;

    public DeliveryLogEvent(Delivery delivery, DeliveryLogType type) {
        this(delivery, type, null);
    }

    public DeliveryLogEvent(Delivery delivery, DeliveryLogType type, String image) {
        this.delivery = delivery;
        this.type = type;
        this.image = image;
    }
}
