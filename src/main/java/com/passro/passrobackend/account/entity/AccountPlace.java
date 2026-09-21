package com.passro.passrobackend.account.entity;


import com.passro.passrobackend.place.entity.Place;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "account_place")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class AccountPlace {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "account_id")
    private Account account;

    @ManyToOne
    @JoinColumn(name = "start_place_id")
    private Place startPlace;

    @ManyToOne
    @JoinColumn(name = "destination_place_id")
    private Place destinationPlace;

    public void changePlace(Place startPlace, Place destinationPlace){
        this.startPlace = startPlace;
        this.destinationPlace = destinationPlace;
    }
}
