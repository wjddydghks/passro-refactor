package com.passro.passrobackend.domain.delivery.location.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ShipperLocation(
        BigDecimal latitude,
        BigDecimal longitude,
        Long placeId,
        LocalDateTime updatedAt
) {
}
