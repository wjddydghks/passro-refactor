package com.passro.passrobackend.place.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.passro.passrobackend.place.entity.Place;
import com.passro.passrobackend.place.repository.PlaceRepository;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PlaceServiceTest {

    @Mock
    private PlaceRepository placeRepository;

    @InjectMocks
    private PlaceService placeService;

    @Test
    void initializeSavesCoordinatesFromSubwayCsv() {
        given(placeRepository.count()).willReturn(0L);

        placeService.initialize();

        List<Place> savedPlaces = capturedPlaces();
        Place seoulStation = findPlace(savedPlaces, "공항", "서울역");
        assertThat(seoulStation.getLatitude()).isEqualByComparingTo("37.55301784");
        assertThat(seoulStation.getLongitude()).isEqualByComparingTo("126.9697643");
    }

    @Test
    void initializeUpdatesMissingCoordinatesForExistingPlaces() {
        Place existingPlace = Place.builder()
                .id(1L)
                .subwayRouteName("공항")
                .subwayStationName("서울역")
                .build();
        given(placeRepository.count()).willReturn(1L);
        given(placeRepository.findAll()).willReturn(List.of(existingPlace));

        placeService.initialize();

        assertThat(existingPlace.getLatitude()).isEqualByComparingTo("37.55301784");
        assertThat(existingPlace.getLongitude()).isEqualByComparingTo("126.9697643");
        then(placeRepository).should().saveAll(List.of(existingPlace));
    }

    @Test
    void initializeCorrectsExistingCoordinatesThatDifferFromSubwayCsv() {
        Place existingPlace = Place.builder()
                .id(1L)
                .subwayRouteName("9호선")
                .subwayStationName("신반포")
                .latitude(new BigDecimal("37.505933"))
                .longitude(new BigDecimal("127.004044"))
                .build();
        given(placeRepository.count()).willReturn(1L);
        given(placeRepository.findAll()).willReturn(List.of(existingPlace));

        placeService.initialize();

        assertThat(existingPlace.getLatitude()).isEqualByComparingTo("37.50358");
        assertThat(existingPlace.getLongitude()).isEqualByComparingTo("126.99643");
        then(placeRepository).should().saveAll(List.of(existingPlace));
    }

    @SuppressWarnings("unchecked")
    private List<Place> capturedPlaces() {
        ArgumentCaptor<Iterable<Place>> captor = ArgumentCaptor.forClass(Iterable.class);
        then(placeRepository).should().saveAll(captor.capture());
        return (List<Place>) captor.getValue();
    }

    private Place findPlace(List<Place> places, String routeName, String stationName) {
        return places.stream()
                .filter(place -> routeName.equals(place.getSubwayRouteName()))
                .filter(place -> stationName.equals(place.getSubwayStationName()))
                .findFirst()
                .orElseThrow();
    }
}
