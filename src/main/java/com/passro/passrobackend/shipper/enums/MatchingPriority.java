package com.passro.passrobackend.shipper.enums;

import lombok.Getter;

@Getter
public enum MatchingPriority {
    RANK_1(1, "출발지 및 목적지 모두 일치"),
    RANK_2(2, "출발지 또는 목적지 중 1개만 일치"),
    RANK_3(3, "통학/출퇴근 통과 역 경로 내 출발지 및 목적지 모두 포함"),
    RANK_4(4, "통학/출퇴근 통과 역 경로 내 출발지 또는 목적지 중 1개만 포함"),
    RANK_5(5, "나머지 요청 (가중치 오름차순)");

    private final int rank;
    private final String description;

    MatchingPriority(int rank, String description) {
        this.rank = rank;
        this.description = description;
    }
}
