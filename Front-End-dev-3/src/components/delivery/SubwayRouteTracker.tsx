import { useMemo } from "react";
import type { SubwayStationItem } from "../../apis/subwayApi";
import type { LocationTrackingStatus } from "../../hooks/useShipperRouteTracking";
import {
    getRouteCoordinateCoverage,
    type GeographicCoordinates,
} from "../../utils/subwayLocation";
import { formatTrackingTime } from "../../utils/deliveryTracking";
import { GeographicRouteMap, getSubwayLineColor } from "./GeographicRouteMap";

interface SubwayRouteTrackerProps {
    stations: SubwayStationItem[];
    currentPlaceId?: number;
    currentCoordinates?: GeographicCoordinates;
    currentDistanceMeters?: number;
    gpsAccuracyMeters?: number;
    trackingStatus: LocationTrackingStatus;
    trackingError?: string | null;
    transferCount: number;
    lastUpdatedAt?: string | null;
    statusMessage?: string;
    variant?: "default" | "figma";
}

function formatDistance(distanceMeters?: number) {
    if (distanceMeters === undefined) {
        return null;
    }

    if (distanceMeters < 1_000) {
        return `${Math.round(distanceMeters)}m`;
    }

    return `${(distanceMeters / 1_000).toFixed(1)}km`;
}

function getTrackingLabel(status: LocationTrackingStatus) {
    switch (status) {
        case "idle":
            return "물품 인수 후 GPS 추적이 시작됩니다.";
        case "locating":
            return "GPS 위치를 확인하고 있습니다.";
        case "active":
            return "GPS를 기준으로 현재 위치를 지도에 표시하고 있습니다.";
        case "denied":
            return "위치 권한이 필요합니다.";
        case "unsupported":
            return "이 브라우저에서는 위치 추적을 사용할 수 없습니다.";
        case "error":
            return "현재 위치를 확인하지 못했습니다.";
    }
}

export function SubwayRouteTracker({
    stations,
    currentPlaceId,
    currentCoordinates,
    currentDistanceMeters,
    gpsAccuracyMeters,
    trackingStatus,
    trackingError,
    transferCount,
    lastUpdatedAt,
    statusMessage,
    variant = "default",
}: SubwayRouteTrackerProps) {
    const currentStationIndex = stations.findIndex(
        (station) => station.id === currentPlaceId,
    );
    const coordinateCoverage = useMemo(
        () => getRouteCoordinateCoverage(stations),
        [stations],
    );

    if (stations.length === 0) {
        return (
            <section className="rounded-2xl bg-gray-50 px-5 py-8 text-center">
                <p className="text-sm font-medium text-gray-500">
                    표시할 지하철 경로가 없습니다.
                </p>
            </section>
        );
    }

    const uniqueRoutes = [
        ...new Set(stations.map((station) => station.routeName)),
    ];
    const currentStation =
        currentStationIndex >= 0 ? stations[currentStationIndex] : null;
    const distanceLabel = formatDistance(currentDistanceMeters);
    const updatedAtLabel = formatTrackingTime(lastUpdatedAt);
    const trackingLabel = statusMessage ?? getTrackingLabel(trackingStatus);

    if (variant === "figma") {
        return (
            <section>
                <h2 className="text-[17px] font-bold leading-[22px] text-gray-900">
                    실시간 전달 지도
                </h2>
                <div className="mt-3 overflow-hidden rounded-[10px]">
                    <GeographicRouteMap
                        stations={stations}
                        currentPlaceId={currentPlaceId}
                        currentCoordinates={currentCoordinates}
                        compact
                    />
                </div>
                {trackingError ? (
                    <p
                        className="mt-2 text-xs font-medium text-rose-600"
                        role="alert"
                    >
                        {trackingError}
                    </p>
                ) : null}
            </section>
        );
    }

    return (
        <section className="overflow-hidden rounded-2xl border border-purple-100 bg-white shadow-sm">
            <div className="flex items-start justify-between gap-3 px-5 pb-3 pt-5">
                <div>
                    <h2 className="font-bold text-gray-900">
                        실시간 전달 지도
                    </h2>
                    <p className="mt-1 text-xs font-medium text-gray-500">
                        {stations.length}개 역 · 환승 {transferCount}회
                    </p>
                </div>
                <span
                    className={`mt-0.5 h-2.5 w-2.5 shrink-0 rounded-full ${
                        trackingStatus === "active"
                            ? "animate-pulse bg-emerald-500"
                            : trackingStatus === "locating"
                              ? "animate-pulse bg-amber-400"
                              : "bg-gray-300"
                    }`}
                    aria-hidden="true"
                />
            </div>

            <GeographicRouteMap
                stations={stations}
                currentPlaceId={currentPlaceId}
                currentCoordinates={currentCoordinates}
            />

            <div className="px-5 py-4">
                <div className="flex flex-wrap gap-2">
                    {uniqueRoutes.map((routeName) => (
                        <span
                            key={routeName}
                            className="inline-flex items-center gap-1.5 rounded-full bg-gray-50 px-2.5 py-1 text-[11px] font-bold text-gray-600"
                        >
                            <span
                                className="h-2 w-2 rounded-full"
                                style={{
                                    backgroundColor:
                                        getSubwayLineColor(routeName),
                                }}
                            />
                            {routeName}
                        </span>
                    ))}
                </div>

                <div className="mt-4 rounded-xl bg-purple-50 px-4 py-3">
                    <p className="text-sm font-bold text-purple-800">
                        {currentStation
                            ? `현재 ${currentStation.stationName} 인근`
                            : trackingLabel}
                    </p>
                    {currentStation && distanceLabel ? (
                        <p className="mt-1 text-xs font-medium text-purple-600">
                            {`역까지 약 ${distanceLabel}${
                                gpsAccuracyMeters !== undefined
                                    ? ` · GPS 오차 ±${Math.round(gpsAccuracyMeters)}m`
                                    : ""
                            }`}
                            {updatedAtLabel ? ` · ${updatedAtLabel} 갱신` : ""}
                        </p>
                    ) : null}
                </div>

                {coordinateCoverage < stations.length ? (
                    <p className="mt-3 text-xs font-medium text-amber-700">
                        좌표 확인 가능 역 {coordinateCoverage}/{stations.length}
                        개
                    </p>
                ) : null}

                {trackingError ? (
                    <p
                        className="mt-3 text-xs font-medium text-rose-600"
                        role="alert"
                    >
                        {trackingError}
                    </p>
                ) : null}
            </div>
        </section>
    );
}
