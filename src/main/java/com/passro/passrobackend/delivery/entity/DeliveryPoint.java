package com.passro.passrobackend.delivery.entity;

import com.passro.passrobackend.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryPoint extends BaseEntity {
    @GeneratedValue
    @Id
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "delivery_id", nullable = false, unique = true)
    private Delivery delivery;

    private Long base_point;
    private Long distance_point;
    private Long weight_point;
}
