package com.passro.passrobackend.account.entity;

import com.passro.passrobackend.place.entity.Place;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "waypoint")
public class WayPoint {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "account_place_id")
    private AccountPlace accountPlace;

    @ManyToOne
    @JoinColumn(name = "place_id")
    private Place place;

    private Integer visitOrder;
}
