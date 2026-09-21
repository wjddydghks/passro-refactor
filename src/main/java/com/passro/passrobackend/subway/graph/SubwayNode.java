package com.passro.passrobackend.subway.graph;

import lombok.Builder;
import lombok.Getter;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Getter
@Builder
public class SubwayNode {
    // Related with Place entity
    private final Long id;
    private final String region;
    private final String name;
    private final String route;
    private final BigDecimal latitude;
    private final BigDecimal longitude;

    @Builder.Default
    private final List<SubwayEdge> edges = new ArrayList<>();

    public void addEdge(SubwayEdge edge) {
        edges.add(edge);
    }
}
