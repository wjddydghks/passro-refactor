package com.passro.passrobackend.subway.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SubwayTravelTimeCalculatorTest {

    @Test
    void convertsRouteWeightToEstimatedMinutesUsingExistingPolicy() {
        assertThat(SubwayTravelTimeCalculator.toEstimatedTimeMinutes(7)).isEqualTo(21);
    }
}
