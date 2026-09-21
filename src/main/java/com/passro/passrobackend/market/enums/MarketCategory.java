package com.passro.passrobackend.market.enums;

import java.util.Arrays;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum MarketCategory {
    FOOD("음식"),
    CAFE("카페"),
    CONVENIENCE_STORE("편의점"),
    ETC("기타");

    private final String label;

    public static MarketCategory from(String value) {
        return Arrays.stream(values())
                .filter(category -> category.label.equals(value) || category.name().equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(IllegalArgumentException::new);
    }
}
