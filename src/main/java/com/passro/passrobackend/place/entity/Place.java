package com.passro.passrobackend.place.entity;

import com.passro.passrobackend.global.entity.BaseEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Place extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String subwayRouteName;

    private String subwayStationName;

    @Column(precision = 11, scale = 8)
    private BigDecimal latitude;

    @Column(precision = 11, scale = 8)
    private BigDecimal longitude;

    public void updateCoordinates(BigDecimal latitude, BigDecimal longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

}
