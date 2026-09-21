package com.passro.passrobackend.subway;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.passro.passrobackend.place.entity.Place;
import com.passro.passrobackend.subway.dto.SubwayRouteResponseDto;
import com.passro.passrobackend.subway.graph.SubwayEdge;
import com.passro.passrobackend.subway.graph.SubwayNode;
import com.passro.passrobackend.subway.service.SubwayService;
import com.passro.passrobackend.support.IntegrationTestSupport;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

class SubwayServiceIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private SubwayService subwayService;

    @Test
    void sameStationOnDifferentRoutesUsesDistinctNodesAndTransferEdges() {
        SubwayNode bundangJeongja = requiredNode("수인분당", "정자");
        SubwayNode shinbundangJeongja = requiredNode("신분당", "정자");

        assertThat(bundangJeongja.getId()).isNotEqualTo(shinbundangJeongja.getId());
        assertThat(edgeBetween(bundangJeongja, shinbundangJeongja).isCrossroute()).isTrue();
        assertThat(edgeBetween(shinbundangJeongja, bundangJeongja).isCrossroute()).isTrue();
    }

    @Test
    void sameNamedStationsFarApartAreNotConnectedAsTransfers() {
        assertNoEdgeBetween(requiredNode("경의중앙", "양평"), requiredNode("5호선", "양평"));
        assertNoEdgeBetween(requiredNode("부산 1호선", "좌천"), requiredNode("부산 동해", "좌천"));
        assertNoEdgeBetween(requiredNode("경의중앙", "신촌"), requiredNode("2호선", "신촌"));
    }

    @Test
    void routeCanContinueThroughJeongjaTransfer() {
        SubwayNode bundangMigeum = requiredNode("수인분당", "미금(분당서울대병원)");
        SubwayNode bundangJeongja = requiredNode("수인분당", "정자");
        SubwayNode shinbundangJeongja = requiredNode("신분당", "정자");
        SubwayNode shinbundangPangyo = requiredNode("신분당", "판교(판교테크노밸리)");

        assertThat(edgeBetween(bundangMigeum, bundangJeongja).isCrossroute()).isFalse();
        assertThat(edgeBetween(bundangJeongja, shinbundangJeongja).isCrossroute()).isTrue();
        assertThat(edgeBetween(shinbundangJeongja, shinbundangPangyo).isCrossroute()).isFalse();
    }

    @Test
    void nonCapitalRoutesUseRegionPrefix() {
        SubwayNode capitalCityHall = requiredNode("1호선", "시청");
        SubwayNode daejeonCityHall = requiredNode("대전 1호선", "시청");

        assertThat(capitalCityHall.getId()).isNotEqualTo(daejeonCityHall.getId());
        assertThat(capitalCityHall.getEdges())
                .noneMatch(edge -> edge.getTarget().getId().equals(daejeonCityHall.getId()));
        assertThat(subwayService.findNode("대구 1호선", "교대")).isPresent();
        assertThat(subwayService.findNode("부산 1호선", "교대")).isPresent();
    }

    @Test
    void dijkstraReturnsDistanceAndTransferCount() {
        Place origin = place("수인분당", "미금(분당서울대병원)");
        Place destination = place("신분당", "판교(판교테크노밸리)");

        SubwayRouteResponseDto result = subwayService.findShortestRoute(origin, null, destination);

        assertThat(result.getShortestDistance()).isEqualTo(5);
        assertThat(result.getTransferCount()).isEqualTo(1);
    }

    @Test
    void dijkstraReturnsEveryStationInTravelOrder() {
        Place origin = place("수인분당", "미금(분당서울대병원)");
        Place destination = place("신분당", "판교(판교테크노밸리)");

        SubwayRouteResponseDto result = subwayService.findShortestRoute(origin, null, destination);

        assertThat(result.getStations())
                .extracting(station -> station.getRouteName() + "|" + station.getStationName())
                .containsExactly(
                        "수인분당|미금(분당서울대병원)",
                        "수인분당|정자",
                        "신분당|정자",
                        "신분당|판교(판교테크노밸리)");
        assertThat(result.getStations()).hasSize(4);
    }

    @Test
    void waypointSegmentBoundariesAreNotDuplicatedInStationList() {
        Place origin = place("수인분당", "미금(분당서울대병원)");
        Place waypoint = place("신분당", "정자");
        Place destination = place("신분당", "판교(판교테크노밸리)");

        SubwayRouteResponseDto result = subwayService.findShortestRoute(
                origin, List.of(waypoint), destination);

        assertThat(result.getStations())
                .extracting(station -> station.getId())
                .containsExactly(
                        origin.getId(),
                        requiredNode("수인분당", "정자").getId(),
                        waypoint.getId(),
                        destination.getId());
    }

    @Test
    void shortestRouteControllerReturnsDistanceTransfersAndStations() throws Exception {
        Place origin = place("수인분당", "미금(분당서울대병원)");
        Place waypoint = place("신분당", "정자");
        Place destination = place("신분당", "판교(판교테크노밸리)");
        String request = objectMapper.writeValueAsString(Map.of(
                "originPlaceId", origin.getId(),
                "waypointPlaceIds", List.of(waypoint.getId()),
                "destinationPlaceId", destination.getId()));

        mockMvc.perform(post("/subway/routes/shortest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("SUBWAY200_1"))
                .andExpect(jsonPath("$.result.shortestDistance").value(5))
                .andExpect(jsonPath("$.result.transferCount").value(1))
                .andExpect(jsonPath("$.result.stations.length()").value(4))
                .andExpect(jsonPath("$.result.stations[0].id").value(origin.getId()))
                .andExpect(jsonPath("$.result.stations[1].routeName").value("수인분당"))
                .andExpect(jsonPath("$.result.stations[1].stationName").value("정자"))
                .andExpect(jsonPath("$.result.stations[2].routeName").value("신분당"))
                .andExpect(jsonPath("$.result.stations[2].stationName").value("정자"))
                .andExpect(jsonPath("$.result.stations[3].id").value(destination.getId()));
    }

    @Test
    void shortestRouteControllerRejectsMissingRequiredPlaceId() throws Exception {
        mockMvc.perform(post("/subway/routes/shortest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"waypointPlaceIds\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("COMMON400"))
                .andExpect(jsonPath("$.result.originPlaceId").exists())
                .andExpect(jsonPath("$.result.destinationPlaceId").exists());
    }

    @Test
    void shortestRouteControllerReturnsNotFoundForUnknownPlaceId() throws Exception {
        Place origin = place("신분당", "정자");
        String request = objectMapper.writeValueAsString(Map.of(
                "originPlaceId", origin.getId(),
                "waypointPlaceIds", List.of(),
                "destinationPlaceId", Long.MAX_VALUE));

        mockMvc.perform(post("/subway/routes/shortest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("SUBWAY404_1"));
    }

    @Test
    void openApiDocumentsSubwayResourcesUnderOneTag() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/subway/search'].get.tags[0]").value("지하철"))
                .andExpect(jsonPath("$.paths['/subway/search'].get.security").isEmpty())
                .andExpect(jsonPath("$.paths['/subway/routes/shortest'].post.tags[0]").value("지하철"))
                .andExpect(jsonPath("$.paths['/subway/routes/shortest'].post.security").isEmpty())
                .andExpect(jsonPath("$.components.schemas.SubwayRouteResponseDto.type").value("object"))
                .andExpect(jsonPath(
                        "$.components.schemas.APIResponseSubwayRouteResponseDto.properties.result['$ref']")
                        .value("#/components/schemas/SubwayRouteResponseDto"))
                .andExpect(jsonPath(
                        "$.paths['/sender/{deliveryId}/routes/shipper-commute'].get.responses['200'].content['*/*'].schema['$ref']")
                        .value("#/components/schemas/APIResponseSubwayRouteResponseDto"))
                .andExpect(jsonPath(
                        "$.paths['/sender/{deliveryId}/routes/shipper-commute'].get.responses['403'].content['*/*'].schema['$ref']")
                        .value("#/components/schemas/APIResponse"))
                .andExpect(jsonPath(
                        "$.paths['/sender/{deliveryId}/routes/shipper-commute'].get.responses['403'].content['*/*'].examples.DELIVERY403_1.value.code")
                        .value("DELIVERY403_1"))
                .andExpect(jsonPath(
                        "$.paths['/sender/{deliveryId}/routes/shipper-commute'].get.responses['404'].content['*/*'].examples.DELIVERY404_4.value.code")
                        .value("DELIVERY404_4"))
                .andExpect(jsonPath(
                        "$.paths['/sender/{deliveryId}/routes/shipper-commute'].get.responses['404'].content['*/*'].examples.SUBWAY404_2.value.code")
                        .value("SUBWAY404_2"))
                .andExpect(jsonPath("$.paths['/subway/stations']").doesNotExist());
    }

    @Test
    void waypointsAreVisitedInGivenOrder() {
        Place origin = place("신분당", "신사");
        Place firstWaypoint = place("신분당", "정자");
        Place secondWaypoint = place("신분당", "강남");
        Place destination = place("신분당", "판교(판교테크노밸리)");

        SubwayRouteResponseDto result = subwayService.findShortestRoute(
                origin, List.of(firstWaypoint, secondWaypoint), destination);
        SubwayRouteResponseDto firstSegment = subwayService.findShortestRoute(origin, null, firstWaypoint);
        SubwayRouteResponseDto secondSegment = subwayService.findShortestRoute(firstWaypoint, null, secondWaypoint);
        SubwayRouteResponseDto thirdSegment = subwayService.findShortestRoute(secondWaypoint, null, destination);

        assertThat(result.getShortestDistance()).isEqualTo(
                firstSegment.getShortestDistance()
                        + secondSegment.getShortestDistance()
                        + thirdSegment.getShortestDistance());
        assertThat(result.getTransferCount()).isEqualTo(
                firstSegment.getTransferCount()
                        + secondSegment.getTransferCount()
                        + thirdSegment.getTransferCount());
    }

    @Test
    void sameOriginAndDestinationReturnsZero() {
        Place place = place("신분당", "정자");

        SubwayRouteResponseDto result = subwayService.findShortestRoute(place, List.of(), place);

        assertThat(result.getShortestDistance()).isZero();
        assertThat(result.getTransferCount()).isZero();
    }

    @Test
    void nullAndEmptyWaypointsProduceTheSameRoute() {
        Place origin = place("수인분당", "미금(분당서울대병원)");
        Place destination = place("신분당", "판교(판교테크노밸리)");

        SubwayRouteResponseDto withNull = subwayService.findShortestRoute(origin, null, destination);
        SubwayRouteResponseDto withEmptyList = subwayService.findShortestRoute(origin, List.of(), destination);

        assertThat(withNull.getShortestDistance()).isEqualTo(withEmptyList.getShortestDistance());
        assertThat(withNull.getTransferCount()).isEqualTo(withEmptyList.getTransferCount());
    }

    @Test
    void repeatedWaypointDoesNotAddDistanceOrTransfers() {
        Place origin = place("신분당", "신사");
        Place destination = place("신분당", "판교(판교테크노밸리)");

        SubwayRouteResponseDto direct = subwayService.findShortestRoute(origin, null, destination);
        SubwayRouteResponseDto repeated = subwayService.findShortestRoute(
                origin, List.of(origin, origin), destination);

        assertThat(repeated.getShortestDistance()).isEqualTo(direct.getShortestDistance());
        assertThat(repeated.getTransferCount()).isEqualTo(direct.getTransferCount());
    }

    @Test
    void placeWithoutIdIsResolvedByRouteAndStationName() {
        Place origin = Place.builder()
                .subwayRouteName("수인분당")
                .subwayStationName("미금(분당서울대병원)")
                .build();
        Place destination = Place.builder()
                .subwayRouteName("신분당")
                .subwayStationName("판교(판교테크노밸리)")
                .build();

        SubwayRouteResponseDto result = subwayService.findShortestRoute(origin, null, destination);

        assertThat(result.getShortestDistance()).isEqualTo(5);
        assertThat(result.getTransferCount()).isEqualTo(1);
    }

    @Test
    void multipleTransfersAreCountedAcrossTheRoute() {
        Place origin = place("수인분당", "미금(분당서울대병원)");
        Place waypoint = place("신분당", "정자");
        Place destination = place("경강", "판교(판교테크노밸리)");

        SubwayRouteResponseDto result = subwayService.findShortestRoute(
                origin, List.of(waypoint), destination);

        assertThat(result.getShortestDistance()).isEqualTo(8);
        assertThat(result.getTransferCount()).isEqualTo(2);
    }

    @Test
    void unknownPlaceIsRejected() {
        Place origin = place("신분당", "정자");
        Place unknown = Place.builder()
                .id(Long.MAX_VALUE)
                .subwayRouteName("없는 노선")
                .subwayStationName("없는 역")
                .build();

        assertThatThrownBy(() -> subwayService.findShortestRoute(origin, null, unknown))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("지하철 Node");
    }

    @Test
    void nullOriginDestinationAndWaypointAreRejected() {
        Place place = place("신분당", "정자");

        assertThatThrownBy(() -> subwayService.findShortestRoute(null, null, place))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> subwayService.findShortestRoute(place, null, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> subwayService.findShortestRoute(
                        place, Arrays.asList((Place) null), place))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void stationsInDifferentRegionsAreDisconnected() {
        Place capitalCityHall = place("1호선", "시청");
        Place daejeonCityHall = place("대전 1호선", "시청");

        assertThatThrownBy(() -> subwayService.findShortestRoute(
                        capitalCityHall, null, daejeonCityHall))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("경로");
    }

    @Test
    void lineOneBranchesDoNotCreateCrossBranchGhostEdges() {
        assertThat(adjacentStationNames("1호선", "주안"))
                .containsExactlyInAnyOrder("간석", "도화");
        assertThat(adjacentStationNames("1호선", "수원"))
                .containsExactlyInAnyOrder("화서", "세류");
        assertThat(adjacentStationNames("1호선", "구로"))
                .containsExactlyInAnyOrder("신도림", "가산디지털단지", "구일");

        SubwayRouteResponseDto route = subwayService.findShortestRoute(
                place("1호선", "주안"), null, place("1호선", "수원"));

        assertThat(route.getShortestDistance()).isGreaterThan(1);
        assertThat(route.getStations())
                .extracting(station -> station.getStationName())
                .contains("구로");
    }

    @Test
    void exceptionalBranchLinesUseOnlyRealAdjacentStations() {
        assertThat(adjacentStationNames("2호선", "시청"))
                .containsExactlyInAnyOrder("충정로(경기대입구)", "을지로입구");
        assertThat(adjacentStationNames("2호선", "신도림"))
                .containsExactlyInAnyOrder("문래", "대림(구로구청)", "도림천");

        assertThat(adjacentStationNames("5호선", "강동"))
                .containsExactlyInAnyOrder("천호(풍납토성)", "길동", "둔촌동");
        assertThat(adjacentStationNames("5호선", "길동"))
                .doesNotContain("올림픽공원(한국체대)");

        assertThat(adjacentStationNames("경의중앙", "가좌"))
                .containsExactlyInAnyOrder("디지털미디어시티", "홍대입구", "신촌");

        assertThat(adjacentStationNames("경춘", "상봉"))
                .containsExactlyInAnyOrder("중랑", "광운대", "망우");
        assertThat(adjacentStationNames("경춘", "광운대"))
                .containsExactly("상봉");
    }

    @Test
    void gtxAConnectsOperationalSegmentsButKeepsSuseoAndSeoulDisconnected() {
        assertThat(adjacentStationNames("GTX-A", "동탄"))
                .containsExactly("구성");
        assertThat(adjacentStationNames("GTX-A", "구성"))
                .containsExactlyInAnyOrder("동탄", "성남");
        assertThat(adjacentStationNames("GTX-A", "성남"))
                .containsExactlyInAnyOrder("구성", "수서");
        assertThat(adjacentStationNames("GTX-A", "수서"))
                .containsExactly("성남");

        assertNoEdgeBetween(
                requiredNode("GTX-A", "수서"),
                requiredNode("GTX-A", "서울역"));

        assertThat(adjacentStationNames("GTX-A", "서울역"))
                .containsExactly("연신내");
        assertThat(adjacentStationNames("GTX-A", "운정중앙"))
                .containsExactly("킨텍스");
    }

    @Test
    void everyCorrectedBranchingRouteRemainsConnected() {
        assertSameRouteConnected("1호선");
        assertSameRouteConnected("2호선");
        assertSameRouteConnected("5호선");
        assertSameRouteConnected("경의중앙");
        assertSameRouteConnected("경춘");
    }

    @Test
    void stationLookupReturnsEveryRouteSpecificNode() {
        List<SubwayNode> jeongjaNodes = subwayService.findNodesByStationName("정자");

        assertThat(jeongjaNodes)
                .extracting(SubwayNode::getRoute)
                .contains("수인분당", "신분당");
        assertThat(jeongjaNodes)
                .extracting(SubwayNode::getId)
                .doesNotHaveDuplicates();
    }

    @Test
    void graphNodesAreUniqueAndBackedByValidPlaceData() {
        Collection<SubwayNode> nodes = subwayService.getNodes();

        assertThat(nodes).hasSize(1103);
        assertThat(nodes).extracting(SubwayNode::getId).doesNotHaveDuplicates();
        assertThat(nodes).allSatisfy(node -> {
            assertThat(node.getId()).isNotNull();
            assertThat(node.getRegion()).isNotBlank();
            assertThat(node.getName()).isNotBlank();
            assertThat(node.getRoute()).isNotBlank();
        });

        Set<String> routeAndStationKeys = new HashSet<>();
        assertThat(nodes).allSatisfy(node ->
                assertThat(routeAndStationKeys.add(node.getRoute() + "\u0000" + node.getName())).isTrue());
    }

    @Test
    void everyEdgeFollowsNormalOrTransferEdgeRules() {
        assertThat(subwayService.getEdges()).allSatisfy(edge -> {
            SubwayNode source = edge.getSource();
            SubwayNode target = edge.getTarget();

            assertThat(source.getId()).isNotEqualTo(target.getId());
            assertThat(source.getEdges()).contains(edge);

            if (edge.isCrossroute()) {
                assertThat(edge.getCost()).isEqualTo(3);
                assertThat(source.getRegion()).isEqualTo(target.getRegion());
                assertThat(source.getName()).isEqualTo(target.getName());
                assertThat(source.getRoute()).isNotEqualTo(target.getRoute());
            } else {
                assertThat(edge.getCost()).isEqualTo(1);
                assertThat(source.getRoute()).isEqualTo(target.getRoute());
                assertThat(source.getName()).isNotEqualTo(target.getName());
            }
        });
    }

    @Test
    void everyDirectedEdgeHasExactlyOneMatchingReverseEdge() {
        List<SubwayEdge> edges = subwayService.getEdges();
        Set<EdgeKey> edgeKeys = new HashSet<>();

        for (SubwayEdge edge : edges) {
            assertThat(edgeKeys.add(EdgeKey.from(edge))).isTrue();
        }
        for (SubwayEdge edge : edges) {
            EdgeKey reverse = new EdgeKey(
                    edge.getTarget().getId(),
                    edge.getSource().getId(),
                    edge.getCost(),
                    edge.isCrossroute());
            assertThat(edgeKeys).contains(reverse);
        }
    }

    private SubwayNode requiredNode(String routeName, String stationName) {
        return subwayService.findNode(routeName, stationName).orElseThrow();
    }

    private Place place(String routeName, String stationName) {
        SubwayNode node = requiredNode(routeName, stationName);
        return Place.builder()
                .id(node.getId())
                .subwayRouteName(routeName)
                .subwayStationName(stationName)
                .build();
    }

    private SubwayEdge edgeBetween(SubwayNode source, SubwayNode target) {
        return source.getEdges().stream()
                .filter(edge -> edge.getTarget().getId().equals(target.getId()))
                .findFirst()
                .orElseThrow();
    }

    private void assertNoEdgeBetween(SubwayNode source, SubwayNode target) {
        assertThat(source.getEdges())
                .noneMatch(edge -> edge.getTarget().getId().equals(target.getId()));
        assertThat(target.getEdges())
                .noneMatch(edge -> edge.getTarget().getId().equals(source.getId()));
    }

    private Set<String> adjacentStationNames(String routeName, String stationName) {
        return requiredNode(routeName, stationName).getEdges().stream()
                .filter(edge -> !edge.isCrossroute())
                .map(edge -> edge.getTarget().getName())
                .collect(java.util.stream.Collectors.toSet());
    }

    private void assertSameRouteConnected(String routeName) {
        List<SubwayNode> routeNodes = subwayService.getNodes().stream()
                .filter(node -> node.getRoute().equals(routeName))
                .toList();
        Set<Long> visitedNodeIds = new HashSet<>();
        ArrayDeque<SubwayNode> queue = new ArrayDeque<>();
        queue.add(routeNodes.getFirst());

        while (!queue.isEmpty()) {
            SubwayNode current = queue.removeFirst();
            if (!visitedNodeIds.add(current.getId())) {
                continue;
            }
            current.getEdges().stream()
                    .filter(edge -> !edge.isCrossroute())
                    .map(SubwayEdge::getTarget)
                    .filter(node -> node.getRoute().equals(routeName))
                    .filter(node -> !visitedNodeIds.contains(node.getId()))
                    .forEach(queue::addLast);
        }

        assertThat(visitedNodeIds)
                .as("%s 노선의 모든 역이 연결되어야 합니다.", routeName)
                .containsExactlyInAnyOrderElementsOf(routeNodes.stream().map(SubwayNode::getId).toList());
    }

    private record EdgeKey(Long sourceId, Long targetId, int cost, boolean crossroute) {

        private static EdgeKey from(SubwayEdge edge) {
            return new EdgeKey(
                    edge.getSource().getId(),
                    edge.getTarget().getId(),
                    edge.getCost(),
                    edge.isCrossroute());
        }
    }
}
