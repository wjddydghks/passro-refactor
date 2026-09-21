package com.passro.passrobackend.subway.service;

public final class SubwayTravelTimeCalculator {

    private static final int MINUTES_PER_ROUTE_WEIGHT = 3;

    private SubwayTravelTimeCalculator() {
    }

    public static int toEstimatedTimeMinutes(int routeWeight) {
        return Math.multiplyExact(routeWeight, MINUTES_PER_ROUTE_WEIGHT);
    }
}
