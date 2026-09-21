package com.passro.passrobackend.delivery.configuration;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "delivery.point")
public class DeliveryPointProperties {

    private long base = 2000L;
    private int inRouteStationLimit = 10;
    private long inRouteOverLimit = 200L;
    private Map<String, Long> size = new HashMap<>(Map.of(
            "S", 0L,
            "M", 500L,
            "L", 1000L
    ));

    public long pointForRoute(int stationCount) {
        return stationCount <= inRouteStationLimit ? 0L : inRouteOverLimit;
    }

    public long pointForSize(String deliverySize) {
        Long point = size.get(deliverySize.toUpperCase(Locale.ROOT));
        if (point == null) {
            throw new IllegalArgumentException("지원하지 않는 배송 크기입니다: " + deliverySize);
        }
        return point;
    }
}
