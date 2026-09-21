package com.passro.passrobackend.place.service;

import com.passro.passrobackend.place.entity.Place;
import com.passro.passrobackend.place.repository.PlaceRepository;
import jakarta.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlaceService {

    private static final String SUBWAY_DATA_PATH = "subway_station_data.csv";
    private static final Charset SUBWAY_DATA_CHARSET = Charset.forName("MS949");
    private static final String CAPITAL_REGION = "수도권";
    private static final String REGION_COLUMN = "권역명";
    private static final String ROUTE_COLUMN = "노선명";
    private static final String STATION_COLUMN = "역명";
    private static final String LATITUDE_COLUMN = "위도";
    private static final String LONGITUDE_COLUMN = "경도";

    private final PlaceRepository placeRepository;

    @PostConstruct
    public void initialize() {
        long placeCount = placeRepository.count();
        List<Place> places = readSubwayPlaces();
        if (placeCount == 0) {
            placeRepository.saveAll(places);
            log.info("지하철역 초기 데이터 {}건을 저장했습니다.", places.size());
            return;
        }

        synchronizeCoordinates(places);
    }

    public List<Place> searchByKeyword(String keyword) {
        return placeRepository
                .findAllBySubwayRouteNameContainingIgnoreCaseOrSubwayStationNameContainingIgnoreCase(
                        keyword, keyword);
    }

    private List<Place> readSubwayPlaces() {
        ClassPathResource resource = new ClassPathResource(SUBWAY_DATA_PATH);

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), SUBWAY_DATA_CHARSET))) {
            String headerLine = reader.readLine();
            if (headerLine == null) {
                throw new IllegalStateException("지하철역 CSV 파일이 비어 있습니다: " + SUBWAY_DATA_PATH);
            }

            List<String> headers = parseCsvLine(removeBom(headerLine));
            int regionIndex = findColumnIndex(headers, REGION_COLUMN);
            int routeIndex = findColumnIndex(headers, ROUTE_COLUMN);
            int stationIndex = findColumnIndex(headers, STATION_COLUMN);
            int latitudeIndex = findColumnIndex(headers, LATITUDE_COLUMN);
            int longitudeIndex = findColumnIndex(headers, LONGITUDE_COLUMN);
            int requiredColumnCount = List.of(
                            regionIndex, routeIndex, stationIndex, latitudeIndex, longitudeIndex)
                    .stream()
                    .mapToInt(Integer::intValue)
                    .max()
                    .orElseThrow() + 1;

            Map<String, Place> uniquePlaces = new LinkedHashMap<>();
            String line;
            int lineNumber = 1;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.isBlank()) {
                    continue;
                }

                List<String> columns = parseCsvLine(line);
                if (columns.size() < requiredColumnCount) {
                    throw new IllegalStateException("지하철역 CSV " + lineNumber + "행의 컬럼 수가 부족합니다.");
                }

                String regionName = columns.get(regionIndex).trim();
                String originalRouteName = columns.get(routeIndex).trim();
                String stationName = columns.get(stationIndex).trim();
                if (regionName.isEmpty() || originalRouteName.isEmpty() || stationName.isEmpty()) {
                    throw new IllegalStateException(
                            "지하철역 CSV " + lineNumber + "행의 권역명, 노선명 또는 역명이 비어 있습니다.");
                }

                BigDecimal latitude = parseCoordinate(
                        columns.get(latitudeIndex), LATITUDE_COLUMN, lineNumber,
                        BigDecimal.valueOf(-90), BigDecimal.valueOf(90));
                BigDecimal longitude = parseCoordinate(
                        columns.get(longitudeIndex), LONGITUDE_COLUMN, lineNumber,
                        BigDecimal.valueOf(-180), BigDecimal.valueOf(180));

                String routeName = normalizeRouteName(regionName, originalRouteName);
                String key = routeName + '\u0000' + stationName;
                uniquePlaces.putIfAbsent(key, Place.builder()
                        .subwayRouteName(routeName)
                        .subwayStationName(stationName)
                        .latitude(latitude)
                        .longitude(longitude)
                        .build());
            }

            return new ArrayList<>(uniquePlaces.values());
        } catch (IOException exception) {
            throw new IllegalStateException("지하철역 CSV 파일을 읽을 수 없습니다: " + SUBWAY_DATA_PATH, exception);
        }
    }

    private void synchronizeCoordinates(List<Place> parsedPlaces) {
        Map<String, Place> parsedPlaceByKey = new LinkedHashMap<>();
        for (Place place : parsedPlaces) {
            parsedPlaceByKey.put(placeKey(place), place);
        }

        List<Place> updatedPlaces = placeRepository.findAll().stream()
                .filter(place -> parsedPlaceByKey.containsKey(placeKey(place)))
                .filter(place -> hasDifferentCoordinates(
                        place, parsedPlaceByKey.get(placeKey(place))))
                .peek(place -> {
                    Place parsedPlace = parsedPlaceByKey.get(placeKey(place));
                    place.updateCoordinates(parsedPlace.getLatitude(), parsedPlace.getLongitude());
                })
                .toList();

        if (!updatedPlaces.isEmpty()) {
            placeRepository.saveAll(updatedPlaces);
        }
        log.info("기존 지하철역 {}건의 위도·경도를 동기화했습니다.", updatedPlaces.size());
    }

    private boolean hasDifferentCoordinates(Place existingPlace, Place parsedPlace) {
        return existingPlace.getLatitude() == null
                || existingPlace.getLongitude() == null
                || existingPlace.getLatitude().compareTo(parsedPlace.getLatitude()) != 0
                || existingPlace.getLongitude().compareTo(parsedPlace.getLongitude()) != 0;
    }

    private String placeKey(Place place) {
        return place.getSubwayRouteName() + '\u0000' + place.getSubwayStationName();
    }

    private BigDecimal parseCoordinate(
            String value,
            String columnName,
            int lineNumber,
            BigDecimal minimum,
            BigDecimal maximum) {
        String trimmedValue = value.trim();
        try {
            BigDecimal coordinate = new BigDecimal(trimmedValue);
            if (coordinate.compareTo(minimum) < 0 || coordinate.compareTo(maximum) > 0) {
                throw new IllegalStateException(
                        "지하철역 CSV " + lineNumber + "행의 " + columnName + " 값이 유효 범위를 벗어났습니다.");
            }
            return coordinate;
        } catch (NumberFormatException exception) {
            throw new IllegalStateException(
                    "지하철역 CSV " + lineNumber + "행의 " + columnName + " 값이 올바르지 않습니다.", exception);
        }
    }

    private String normalizeRouteName(String regionName, String routeName) {
        if (CAPITAL_REGION.equals(regionName)) {
            return routeName;
        }
        return regionName + " " + routeName;
    }

    private int findColumnIndex(List<String> headers, String columnName) {
        for (int index = 0; index < headers.size(); index++) {
            if (columnName.equals(headers.get(index).trim())) {
                return index;
            }
        }
        throw new IllegalStateException("지하철역 CSV에 '" + columnName + "' 컬럼이 없습니다.");
    }

    private String removeBom(String line) {
        return line.startsWith("\uFEFF") ? line.substring(1) : line;
    }

    private List<String> parseCsvLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder value = new StringBuilder();
        boolean quoted = false;

        for (int index = 0; index < line.length(); index++) {
            char current = line.charAt(index);
            if (current == '"') {
                if (quoted && index + 1 < line.length() && line.charAt(index + 1) == '"') {
                    value.append('"');
                    index++;
                } else {
                    quoted = !quoted;
                }
            } else if (current == ',' && !quoted) {
                values.add(value.toString());
                value.setLength(0);
            } else {
                value.append(current);
            }
        }

        if (quoted) {
            throw new IllegalStateException("지하철역 CSV에 닫히지 않은 따옴표가 있습니다.");
        }

        values.add(value.toString());
        return values;
    }
}
