package com.passro.passrobackend.delivery.location.repository;

import com.passro.passrobackend.delivery.location.model.ShipperLocation;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ShipperLocationRepository {

    private static final String KEY_PREFIX = "shipper:location:";
    private static final Duration LOCATION_TTL = Duration.ofMinutes(2);

    private final StringRedisTemplate redisTemplate;

    public void save(Long shipperId, ShipperLocation location) {
        String key = key(shipperId);
        redisTemplate.opsForHash().putAll(key, Map.of(
                "latitude", location.latitude().toPlainString(),
                "longitude", location.longitude().toPlainString(),
                "placeId", location.placeId().toString(),
                "updatedAt", location.updatedAt().toString()));
        redisTemplate.expire(key, LOCATION_TTL);
    }

    public Optional<ShipperLocation> findByShipperId(Long shipperId) {
        Map<Object, Object> values = redisTemplate.opsForHash().entries(key(shipperId));
        if (values.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(new ShipperLocation(
                new BigDecimal(requiredValue(values, "latitude")),
                new BigDecimal(requiredValue(values, "longitude")),
                Long.valueOf(requiredValue(values, "placeId")),
                LocalDateTime.parse(requiredValue(values, "updatedAt"))));
    }

    private String requiredValue(Map<Object, Object> values, String field) {
        Object value = values.get(field);
        if (value == null) {
            throw new IllegalStateException("배송기사 위치 데이터가 올바르지 않습니다: " + field);
        }
        return value.toString();
    }

    private String key(Long shipperId) {
        return KEY_PREFIX + shipperId;
    }
}
