package com.passro.passrobackend.delivery.entity;

import com.passro.passrobackend.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class DeliveryGoodInfo extends BaseEntity {
    @GeneratedValue
    @Id
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "delivery_id", nullable = false, unique = true)
    private Delivery delivery;

    @Column(nullable = false)
    private String name;

    private Long price;
    private String size;
    private String picture;
}
