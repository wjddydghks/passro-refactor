import type { SubwayStationItem } from "../apis/subwayApi";
import type {
    BackendDeliveryLogInfo,
    BackendDeliveryState,
    BackendPlace,
} from "../types/backend";

export interface DeliveryRouteProgress {
    currentIndex: number;
    currentStation: SubwayStationItem | null;
    nextStation: SubwayStationItem | null;
    completedStationCount: number;
    totalStationCount: number;
    percentage: number;
}

export function formatDeliveryPlace(place: BackendPlace) {
    return `${place.subwayStationName} · ${place.subwayRouteName}`;
}

export function getDeliveryRouteProgress(
    stations: SubwayStationItem[],
    currentPlaceId: number | undefined,
    status: BackendDeliveryState,
): DeliveryRouteProgress {
    const totalStationCount = stations.length;
    const isTravelFinished =
        status === "CONFIRM_REQUESTED" || status === "DELIVERED";
    const currentIndex = isTravelFinished
        ? totalStationCount - 1
        : stations.findIndex((station) => station.id === currentPlaceId);
    const currentStation =
        currentIndex >= 0 ? stations[currentIndex] ?? null : null;
    const nextStation =
        !isTravelFinished && currentIndex >= 0
            ? stations[currentIndex + 1] ?? null
            : null;
    const completedStationCount =
        currentIndex >= 0 ? currentIndex + 1 : 0;
    const percentage =
        totalStationCount <= 1
            ? isTravelFinished
                ? 100
                : 0
            : Math.round(
                  (Math.max(0, currentIndex) /
                      Math.max(1, totalStationCount - 1)) *
                      100,
              );

    return {
        currentIndex,
        currentStation,
        nextStation,
        completedStationCount,
        totalStationCount,
        percentage,
    };
}

export function getDeliveryCompletedAt(logs: BackendDeliveryLogInfo[]) {
    return [...logs].reverse().find((log) => log.type === "DONE")?.createdAt ?? null;
}

export function getDeliveryStartedAt(logs: BackendDeliveryLogInfo[]) {
    return [...logs]
        .reverse()
        .find((log) => log.type === "PICKED_UP")?.createdAt ?? null;
}

export function getDeliveryHandedOffAt(logs: BackendDeliveryLogInfo[]) {
    return [...logs]
        .reverse()
        .find((log) => log.type === "DELIVERED")?.createdAt ?? null;
}

const TIME_ZONE_SUFFIX_PATTERN = /(?:Z|[+-]\d{2}:?\d{2})$/u;

function parseBackendDateTime(value: string) {
    const normalizedValue = TIME_ZONE_SUFFIX_PATTERN.test(value)
        ? value
        : `${value}Z`;

    return new Date(normalizedValue);
}

export function getDeliveryElapsedMinutes(
    startedAt: string | null | undefined,
    endedAt: string | null | undefined,
    now = Date.now(),
) {
    if (!startedAt) {
        return null;
    }

    const startedTime = parseBackendDateTime(startedAt).getTime();
    const endedTime = endedAt
        ? parseBackendDateTime(endedAt).getTime()
        : now;

    if (Number.isNaN(startedTime) || Number.isNaN(endedTime)) {
        return null;
    }

    return Math.max(0, Math.floor((endedTime - startedTime) / 60_000));
}

export function formatTrackingDateTime(value: string | null | undefined) {
    if (!value) {
        return null;
    }

    const date = parseBackendDateTime(value);
    if (Number.isNaN(date.getTime())) {
        return value;
    }

    return new Intl.DateTimeFormat("ko-KR", {
        year: "numeric",
        month: "2-digit",
        day: "2-digit",
        hour: "2-digit",
        minute: "2-digit",
        hour12: false,
        timeZone: "Asia/Seoul",
    }).format(date);
}

export function formatTrackingTime(value: string | null | undefined) {
    if (!value) {
        return null;
    }

    const date = parseBackendDateTime(value);
    if (Number.isNaN(date.getTime())) {
        return value;
    }

    return new Intl.DateTimeFormat("ko-KR", {
        hour: "2-digit",
        minute: "2-digit",
        second: "2-digit",
        hour12: false,
        timeZone: "Asia/Seoul",
    }).format(date);
}
