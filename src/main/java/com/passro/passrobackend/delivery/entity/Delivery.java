package com.passro.passrobackend.delivery.entity;

import com.passro.passrobackend.account.entity.Account;
import com.passro.passrobackend.delivery.enums.DeliveryState;
import com.passro.passrobackend.global.entity.BaseEntity;
import com.passro.passrobackend.place.entity.Place;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Delivery extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private Place origin;

    @ManyToOne
    private Place dest;

    private String memo;

    @OneToOne(mappedBy = "delivery", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private DeliveryGoodInfo deliveryGoodInfo;

    @OneToOne(mappedBy = "delivery", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private DeliveryPoint deliveryPoint;

    @Enumerated(EnumType.STRING)
    private DeliveryState status;
    private Boolean terms;

    @ManyToOne
    private Account sender;

    @ManyToOne
    private Account shipper;

    public void attachGoodInfo(DeliveryGoodInfo goodInfo) {
        this.deliveryGoodInfo = goodInfo;
        goodInfo.setDelivery(this);
    }

    public void attachPoint(DeliveryPoint point) {
        this.deliveryPoint = point;
        point.setDelivery(this);
    }
}
