package com.passro.passrobackend.domain.place.repository;

import com.passro.passrobackend.domain.place.entity.Place;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlaceRepository extends JpaRepository<Place, Long> {

    List<Place> findAllBySubwayRouteNameContainingIgnoreCaseOrSubwayStationNameContainingIgnoreCase(
            String routeName,
            String stationName
    );

    Optional <Place> findBySubwayRouteNameAndSubwayStationName(
            String subwayRouteName,
            String subwayStationName
    );

    boolean existsByLatitudeIsNullOrLongitudeIsNull();

    List<Place> findAllByLatitudeIsNotNullAndLongitudeIsNotNull();
}
