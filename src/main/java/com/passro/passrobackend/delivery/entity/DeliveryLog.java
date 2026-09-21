package com.passro.passrobackend.delivery.entity;

import com.passro.passrobackend.delivery.enums.DeliveryLogType;
import com.passro.passrobackend.global.entity.BaseEntity;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;


@Entity
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryLog extends BaseEntity {
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    private Long id;

    @ManyToOne
    private Delivery delivery;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeliveryLogType type;

    @Column(length = 500)
    private String image;
}
