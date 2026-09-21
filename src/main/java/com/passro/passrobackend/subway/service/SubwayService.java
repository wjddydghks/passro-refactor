package com.passro.passrobackend.subway.service;
import com.passro.passrobackend.place.entity.Place;
import com.passro.passrobackend.place.repository.PlaceRepository;
import com.passro.passrobackend.subway.dto.SubwayRouteResponseDto;
import com.passro.passrobackend.subway.dto.SubwayStationResponseDto;
import com.passro.passrobackend.subway.graph.SubwayEdge;
import com.passro.passrobackend.subway.graph.SubwayNode;
import jakarta.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.TreeMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
@Service
@DependsOn("placeService")
@RequiredArgsConstructor
@Slf4j
public class SubwayService {
    private static final String SUBWAY_DATA_PATH = "subway_station_data.csv";
    private static final String SUBWAY_BRANCH_EDGE_DATA_PATH = "subway_branch_edges.csv";
    private static final Charset SUBWAY_DATA_CHARSET = Charset.forName("MS949");
    private static final Charset SUBWAY_BRANCH_EDGE_DATA_CHARSET = StandardCharsets.UTF_8;
    private static final String CAPITAL_REGION = "수도권";
    private static final String REGION_COLUMN = "권역명";
    private static final String ROUTE_COLUMN = "노선명";
    private static final String ORDER_COLUMN = "순번";
    private static final String STATION_COLUMN = "역명";
    private static final String SOURCE_STATION_COLUMN = "출발역";
    private static final String TARGET_STATION_COLUMN = "도착역";
    private static final int DEFAULT_EDGE_COST = 1;
    private static final double MAX_TRANSFER_DISTANCE_KILOMETERS = 0.5;
    private static final double EARTH_RADIUS_KILOMETERS = 6371.0;
    private final PlaceRepository placeRepository;
    private final Map<String, SubwayNode> nodesByRouteAndStation = new LinkedHashMap<>();
    private final Map<Long, SubwayNode> nodesById = new HashMap<>();
    private final Map<String, List<SubwayNode>> nodesByStation = new HashMap<>();
    private final Map<String, List<SubwayNode>> nodesByTransferStation = new HashMap<>();
    private final List<SubwayEdge> edges = new ArrayList<>();
    @PostConstruct
    public void initialize() {
        List<StationRecord> records = readStationRecords();
        Map<String, Place> places = loadPlaces();
        createNodes(records, places);
        connectStationsOnSameRoute(records);
        connectBranchStations(readBranchEdgeRecords());
        connectTransferStations();
        log.info("지하철 그래프를 생성했습니다. nodes={}, directedEdges={}",
                nodesByRouteAndStation.size(), edges.size());
    }
    public Collection<SubwayNode> getNodes() {
        return Collections.unmodifiableCollection(nodesByRouteAndStation.values());
    }
    public List<SubwayEdge> getEdges() {
        return List.copyOf(edges);
    }
    public Optional<SubwayNode> findNode(String routeName, String stationName) {
        return Optional.ofNullable(nodesByRouteAndStation.get(nodeKey(routeName, stationName)));
    }
    public List<SubwayNode> findNodesByStationName(String stationName) {
        return List.copyOf(nodesByStation.getOrDefault(stationName, List.of()));
    }
    public String getRegionByPlaceId(Long placeId) {
        if (placeId == null) {
            return null;
        }
        SubwayNode node = nodesById.get(placeId);
        return node != null ? node.getRegion() : null;
    }
    public SubwayRouteResponseDto findShortestRoute(
            Place origin,
            List<Place> waypoints,
            Place destination) {
        RouteResult route = calculateRoute(origin, waypoints, destination);
        List<SubwayStationResponseDto> stations = route.nodes().stream()
                .map(SubwayStationResponseDto::from)
                .toList();
        return new SubwayRouteResponseDto(route.distance(), route.transferCount(), stations);
    }
    public SubwayRouteResponseDto findShortestRouteByPlaceIds(
            Long originPlaceId,
            List<Long> waypointPlaceIds,
            Long destinationPlaceId) {
        Place origin = placeReference(originPlaceId);
        List<Place> waypoints = waypointPlaceIds == null
                ? List.of()
                : waypointPlaceIds.stream().map(this::placeReference).toList();
        Place destination = placeReference(destinationPlaceId);
        return findShortestRoute(origin, waypoints, destination);
    }
    private RouteResult calculateRoute(
            Place origin,
            List<Place> waypoints,
            Place destination) {
        List<Place> stops = new ArrayList<>();
        stops.add(origin);
        if (waypoints != null) {
            stops.addAll(waypoints);
        }
        stops.add(destination);
        int totalDistance = 0;
        int totalTransferCount = 0;
        List<SubwayNode> routeNodes = new ArrayList<>();
        for (int index = 0; index < stops.size() - 1; index++) {
            SubwayNode source = getRequiredNode(stops.get(index));
            SubwayNode target = getRequiredNode(stops.get(index + 1));
            RouteResult segment = dijkstra(source, target);
            totalDistance = Math.addExact(totalDistance, segment.distance());
            totalTransferCount = Math.addExact(totalTransferCount, segment.transferCount());
            if (routeNodes.isEmpty()) {
                routeNodes.addAll(segment.nodes());
            } else {
                routeNodes.addAll(segment.nodes().subList(1, segment.nodes().size()));
            }
        }
        return new RouteResult(totalDistance, totalTransferCount, List.copyOf(routeNodes));
    }
    private Place placeReference(Long placeId) {
        if (placeId == null) {
            throw new IllegalArgumentException("Place ID가 null입니다.");
        }
        return Place.builder().id(placeId).build();
    }
    private Map<String, Place> loadPlaces() {
        Map<String, Place> places = new HashMap<>();
        for (Place place : placeRepository.findAll()) {
            places.put(nodeKey(place.getSubwayRouteName(), place.getSubwayStationName()), place);
        }
        return places;
    }
    private void createNodes(List<StationRecord> records, Map<String, Place> places) {
        for (StationRecord record : records) {
            String key = nodeKey(record.routeName(), record.stationName());
            Place place = places.get(key);
            if (place == null) {
                throw new IllegalStateException(
                        "Place를 찾을 수 없습니다: route=" + record.routeName() + ", station=" + record.stationName());
            }
            SubwayNode node = nodesByRouteAndStation.computeIfAbsent(key, ignored -> SubwayNode.builder()
                    .id(place.getId())
                    .region(record.regionName())
                    .route(record.routeName())
                    .name(record.stationName())
                    .latitude(place.getLatitude())
                    .longitude(place.getLongitude())
                    .build());
            nodesById.putIfAbsent(node.getId(), node);
            nodesByStation.computeIfAbsent(node.getName(), ignored -> new ArrayList<>()).add(node);
            nodesByTransferStation
                    .computeIfAbsent(transferStationKey(node.getRegion(), node.getName()), ignored -> new ArrayList<>())
                    .add(node);
        }
    }
    private RouteResult dijkstra(SubwayNode source, SubwayNode target) {
        if (source.getId().equals(target.getId())) {
            return new RouteResult(0, 0, List.of(source));
        }
        Map<Long, RouteScore> bestScores = new HashMap<>();
        Map<Long, SubwayNode> previousNodes = new HashMap<>();
        PriorityQueue<RouteCandidate> queue = new PriorityQueue<>(
                Comparator.comparingInt(RouteCandidate::distance)
                        .thenComparingInt(RouteCandidate::transferCount));
        RouteScore initialScore = new RouteScore(0, 0);
        bestScores.put(source.getId(), initialScore);
        queue.offer(new RouteCandidate(source, 0, 0));
        while (!queue.isEmpty()) {
            RouteCandidate current = queue.poll();
            RouteScore currentBest = bestScores.get(current.node().getId());
            if (currentBest == null
                    || current.distance() != currentBest.distance()
                    || current.transferCount() != currentBest.transferCount()) {
                continue;
            }
            if (current.node().getId().equals(target.getId())) {
                return new RouteResult(
                        currentBest.distance(),
                        currentBest.transferCount(),
                        reconstructPath(source, target, previousNodes));
            }
            for (SubwayEdge edge : current.node().getEdges()) {
                int nextDistance = Math.addExact(current.distance(), edge.getCost());
                int nextTransferCount = current.transferCount() + (edge.isCrossroute() ? 1 : 0);
                RouteScore nextScore = new RouteScore(nextDistance, nextTransferCount);
                Long targetId = edge.getTarget().getId();
                RouteScore previousScore = bestScores.get(targetId);
                if (previousScore == null || nextScore.isBetterThan(previousScore)) {
                    bestScores.put(targetId, nextScore);
                    previousNodes.put(targetId, current.node());
                    queue.offer(new RouteCandidate(edge.getTarget(), nextDistance, nextTransferCount));
                }
            }
        }
        throw new IllegalStateException(
                "지하철 경로를 찾을 수 없습니다: " + describeNode(source) + " -> " + describeNode(target));
    }
    private List<SubwayNode> reconstructPath(
            SubwayNode source,
            SubwayNode target,
            Map<Long, SubwayNode> previousNodes) {
        List<SubwayNode> reversedPath = new ArrayList<>();
        SubwayNode current = target;
        reversedPath.add(current);
        while (!current.getId().equals(source.getId())) {
            current = previousNodes.get(current.getId());
            if (current == null) {
                throw new IllegalStateException("지하철 최단 경로 복원에 실패했습니다.");
            }
            reversedPath.add(current);
        }
        Collections.reverse(reversedPath);
        return List.copyOf(reversedPath);
    }
    private SubwayNode getRequiredNode(Place place) {
        if (place == null) {
            throw new IllegalArgumentException("출발지, 경유지 또는 목적지 Place가 null입니다.");
        }
        if (place.getId() != null) {
            SubwayNode node = nodesById.get(place.getId());
            if (node != null) {
                return node;
            }
        }
        if (place.getSubwayRouteName() != null && place.getSubwayStationName() != null) {
            return findNode(place.getSubwayRouteName(), place.getSubwayStationName())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Place에 해당하는 지하철 Node가 없습니다: route="
                                    + place.getSubwayRouteName() + ", station=" + place.getSubwayStationName()));
        }
        throw new IllegalArgumentException("Place에 지하철 노선명과 역명이 없습니다.");
    }
    private String describeNode(SubwayNode node) {
        return node.getRoute() + " " + node.getName();
    }
    private void connectStationsOnSameRoute(List<StationRecord> records) {
        Map<String, TreeMap<Integer, List<SubwayNode>>> stationsByRouteAndOrder = new LinkedHashMap<>();
        for (StationRecord record : records) {
            SubwayNode node = getRequiredNode(record.routeName(), record.stationName());
            stationsByRouteAndOrder
                    .computeIfAbsent(record.routeName(), ignored -> new TreeMap<>())
                    .computeIfAbsent(record.order(), ignored -> new ArrayList<>())
                    .add(node);
        }
        for (TreeMap<Integer, List<SubwayNode>> stationsByOrder : stationsByRouteAndOrder.values()) {
            Integer previousOrder = null;
            List<SubwayNode> previousNodes = List.of();
            for (Map.Entry<Integer, List<SubwayNode>> entry : stationsByOrder.entrySet()) {
                int currentOrder = entry.getKey();
                if (previousOrder != null
                        && currentOrder == previousOrder + 1
                        && previousNodes.size() == 1
                        && entry.getValue().size() == 1) {
                    connectBidirectionally(previousNodes.getFirst(), entry.getValue().getFirst(), false);
                }
                previousOrder = currentOrder;
                previousNodes = entry.getValue();
            }
        }
    }
    private void connectBranchStations(List<BranchEdgeRecord> records) {
        for (BranchEdgeRecord record : records) {
            SubwayNode source = getRequiredNode(record.routeName(), record.sourceStationName());
            SubwayNode target = getRequiredNode(record.routeName(), record.targetStationName());
            connectBidirectionally(source, target, false);
        }
    }
    private void connectTransferStations() {
        for (List<SubwayNode> stationNodes : nodesByTransferStation.values()) {
            for (int sourceIndex = 0; sourceIndex < stationNodes.size(); sourceIndex++) {
                    SubwayNode source = stationNodes.get(sourceIndex);
                for (int targetIndex = sourceIndex + 1; targetIndex < stationNodes.size(); targetIndex++) {
                    SubwayNode target = stationNodes.get(targetIndex);
                    if (!source.getRoute().equals(target.getRoute())
                            && isWithinTransferDistance(source, target)) {
                        connectBidirectionally(source, target, true, 3);
                        // 환승에 가중치 3 부여
                    }
                }
            }
        }
    }

    private boolean isWithinTransferDistance(SubwayNode source, SubwayNode target) {
        if (source.getLatitude() == null || source.getLongitude() == null
                || target.getLatitude() == null || target.getLongitude() == null) {
            return false;
        }

        double sourceLatitude = Math.toRadians(source.getLatitude().doubleValue());
        double targetLatitude = Math.toRadians(target.getLatitude().doubleValue());
        double latitudeDelta = targetLatitude - sourceLatitude;
        double longitudeDelta = Math.toRadians(
                target.getLongitude().subtract(source.getLongitude()).doubleValue());
        double latitudeComponent = Math.sin(latitudeDelta / 2);
        double longitudeComponent = Math.sin(longitudeDelta / 2);
        double haversine = latitudeComponent * latitudeComponent
                + Math.cos(sourceLatitude) * Math.cos(targetLatitude)
                * longitudeComponent * longitudeComponent;
        double distance = 2 * EARTH_RADIUS_KILOMETERS
                * Math.asin(Math.sqrt(haversine));
        return distance <= MAX_TRANSFER_DISTANCE_KILOMETERS;
    }
    private void connectBidirectionally(SubwayNode first, SubwayNode second, boolean crossroute) {
        addDirectedEdge(first, second, crossroute);
        addDirectedEdge(second, first, crossroute);
    }
    private void connectBidirectionally(SubwayNode first, SubwayNode second, boolean crossroute, int cost) {
        addDirectedEdge(first, second, crossroute, cost);
        addDirectedEdge(second, first, crossroute, cost);
    }
    private void addDirectedEdge(SubwayNode source, SubwayNode target, boolean crossroute) {
        SubwayEdge edge = SubwayEdge.builder()
                .source(source)
                .target(target)
                .cost(DEFAULT_EDGE_COST)
                .crossroute(crossroute)
                .build();
        source.addEdge(edge);
        edges.add(edge);
    }
    private void addDirectedEdge(SubwayNode source, SubwayNode target, boolean crossroute, int cost) {
        SubwayEdge edge = SubwayEdge.builder()
                .source(source)
                .target(target)
                .cost(cost)
                .crossroute(crossroute)
                .build();
        source.addEdge(edge);
        edges.add(edge);
    }
    private SubwayNode getRequiredNode(String routeName, String stationName) {
        return findNode(routeName, stationName)
                .orElseThrow(() -> new IllegalStateException(
                        "지하철 Node를 찾을 수 없습니다: route=" + routeName + ", station=" + stationName));
    }
    private List<StationRecord> readStationRecords() {
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
            int orderIndex = findColumnIndex(headers, ORDER_COLUMN);
            int stationIndex = findColumnIndex(headers, STATION_COLUMN);
            int requiredColumnCount = Math.max(
                    Math.max(regionIndex, routeIndex), Math.max(orderIndex, stationIndex)) + 1;
            List<StationRecord> records = new ArrayList<>();
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
                String orderValue = columns.get(orderIndex).trim();
                String stationName = columns.get(stationIndex).trim();
                if (regionName.isEmpty() || originalRouteName.isEmpty()
                        || orderValue.isEmpty() || stationName.isEmpty()) {
                    throw new IllegalStateException(
                            "지하철역 CSV " + lineNumber + "행의 권역명, 노선명, 순번 또는 역명이 비어 있습니다.");
                }
                int order;
                try {
                    order = Integer.parseInt(orderValue);
                } catch (NumberFormatException exception) {
                    throw new IllegalStateException(
                            "지하철역 CSV " + lineNumber + "행의 순번이 숫자가 아닙니다: " + orderValue,
                            exception);
                }
                records.add(new StationRecord(
                        regionName, normalizeRouteName(regionName, originalRouteName), order, stationName));
            }
            return records;
        } catch (IOException exception) {
            throw new IllegalStateException("지하철역 CSV 파일을 읽을 수 없습니다: " + SUBWAY_DATA_PATH, exception);
        }
    }
    private List<BranchEdgeRecord> readBranchEdgeRecords() {
        ClassPathResource resource = new ClassPathResource(SUBWAY_BRANCH_EDGE_DATA_PATH);
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), SUBWAY_BRANCH_EDGE_DATA_CHARSET))) {
            String headerLine = reader.readLine();
            if (headerLine == null) {
                throw new IllegalStateException("지하철 분기 간선 CSV 파일이 비어 있습니다: "
                        + SUBWAY_BRANCH_EDGE_DATA_PATH);
            }
            List<String> headers = parseCsvLine(removeBom(headerLine));
            int regionIndex = findColumnIndex(headers, REGION_COLUMN);
            int routeIndex = findColumnIndex(headers, ROUTE_COLUMN);
            int sourceStationIndex = findColumnIndex(headers, SOURCE_STATION_COLUMN);
            int targetStationIndex = findColumnIndex(headers, TARGET_STATION_COLUMN);
            int requiredColumnCount = Math.max(
                    Math.max(regionIndex, routeIndex), Math.max(sourceStationIndex, targetStationIndex)) + 1;
            List<BranchEdgeRecord> records = new ArrayList<>();
            String line;
            int lineNumber = 1;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.isBlank()) {
                    continue;
                }
                List<String> columns = parseCsvLine(line);
                if (columns.size() < requiredColumnCount) {
                    throw new IllegalStateException(
                            "지하철 분기 간선 CSV " + lineNumber + "행의 컬럼 수가 부족합니다.");
                }
                String regionName = columns.get(regionIndex).trim();
                String originalRouteName = columns.get(routeIndex).trim();
                String sourceStationName = columns.get(sourceStationIndex).trim();
                String targetStationName = columns.get(targetStationIndex).trim();
                if (regionName.isEmpty() || originalRouteName.isEmpty()
                        || sourceStationName.isEmpty() || targetStationName.isEmpty()) {
                    throw new IllegalStateException(
                            "지하철 분기 간선 CSV " + lineNumber + "행의 값이 비어 있습니다.");
                }
                records.add(new BranchEdgeRecord(
                        normalizeRouteName(regionName, originalRouteName),
                        sourceStationName,
                        targetStationName));
            }
            return records;
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "지하철 분기 간선 CSV 파일을 읽을 수 없습니다: " + SUBWAY_BRANCH_EDGE_DATA_PATH,
                    exception);
        }
    }
    private String normalizeRouteName(String regionName, String routeName) {
        return CAPITAL_REGION.equals(regionName) ? routeName : regionName + " " + routeName;
    }
    private String nodeKey(String routeName, String stationName) {
        return routeName + '\u0000' + stationName;
    }
    private String transferStationKey(String regionName, String stationName) {
        return regionName + '\u0000' + stationName;
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
    private record StationRecord(String regionName, String routeName, int order, String stationName) {
    }
    private record BranchEdgeRecord(String routeName, String sourceStationName, String targetStationName) {
    }
    private record RouteCandidate(SubwayNode node, int distance, int transferCount) {
    }
    private record RouteScore(int distance, int transferCount) {
        private boolean isBetterThan(RouteScore other) {
            return distance < other.distance
                    || (distance == other.distance && transferCount < other.transferCount);
        }
    }
    private record RouteResult(int distance, int transferCount, List<SubwayNode> nodes) {
    }
}
