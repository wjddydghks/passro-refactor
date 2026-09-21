package com.passro.passrobackend.subway.graph;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SubwayEdge {
    private final SubwayNode source;
    private final SubwayNode target;

    private final int cost;

    // 교차 노선 여부
    private final boolean crossroute;
}
