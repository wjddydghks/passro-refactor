package com.passro.passrobackend.market.entity;

import com.passro.passrobackend.global.entity.BaseEntity;
import com.passro.passrobackend.market.enums.MarketCategory;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Getter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Market extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Long price;

    @Column(name = "image_key")
    private String imageKey;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private MarketCategory category = MarketCategory.ETC;

    public MarketCategory categoryOrDefault() {
        return category == null ? MarketCategory.ETC : category;
    }
}
