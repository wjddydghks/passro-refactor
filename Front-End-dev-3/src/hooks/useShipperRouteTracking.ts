import { useEffect, useRef, useState } from "react";
import { locationApi, type LiveLocation } from "../apis/locationApi";
import type { SubwayStationItem } from "../apis/subwayApi";
import {
    findNearestRouteStation,
    type GeographicCoordinates,
    type NearestRouteStation,
} from "../utils/subwayLocation";

const LOCATION_SYNC_INTERVAL_MS = 15_000;

export type LocationTrackingStatus =
    | "idle"
    | "locating"
    | "active"
    | "unsupported"
    | "denied"
    | "error";

interface UseShipperRouteTrackingOptions {
    enabled: boolean;
    stations: SubwayStationItem[];
}

interface BrowserPosition extends GeographicCoordinates {
    accuracyMeters: number;
}

function getGeolocationErrorMessage(error: GeolocationPositionError) {
    switch (error.code) {
        case error.PERMISSION_DENIED:
            return "위치 권한이 거부되었습니다. 브라우저 설정에서 위치 권한을 허용해주세요.";
        case error.POSITION_UNAVAILABLE:
            return "현재 GPS 위치를 확인할 수 없습니다.";
        case error.TIMEOUT:
            return "GPS 위치 확인 시간이 초과되었습니다.";
        default:
            return "GPS 위치를 확인하는 중 오류가 발생했습니다.";
    }
}

export function useShipperRouteTracking({
    enabled,
    stations,
}: UseShipperRouteTrackingOptions) {
    const [status, setStatus] = useState<LocationTrackingStatus>("idle");
    const [position, setPosition] = useState<BrowserPosition | null>(null);
    const [nearestStation, setNearestStation] =
        useState<NearestRouteStation | null>(null);
    const [lastSyncedLocation, setLastSyncedLocation] =
        useState<LiveLocation | null>(null);
    const [errorMessage, setErrorMessage] = useState<string | null>(null);
    const lastSyncRef = useRef<{ placeId: number; syncedAt: number } | null>(
        null,
    );
    const syncInFlightRef = useRef(false);

    useEffect(() => {
        if (!enabled) {
            setStatus("idle");
            setErrorMessage(null);
            return;
        }

        if (!("geolocation" in navigator)) {
            setStatus("unsupported");
            setErrorMessage("이 브라우저는 GPS 위치 추적을 지원하지 않습니다.");
            return;
        }

        let isActive = true;
        setStatus("locating");
        setErrorMessage(null);

        const watchId = navigator.geolocation.watchPosition(
            (geolocationPosition) => {
                if (!isActive) {
                    return;
                }

                const currentPosition: BrowserPosition = {
                    latitude: geolocationPosition.coords.latitude,
                    longitude: geolocationPosition.coords.longitude,
                    accuracyMeters: geolocationPosition.coords.accuracy,
                };
                const nearest = findNearestRouteStation(
                    currentPosition,
                    stations,
                );

                setPosition(currentPosition);
                setNearestStation(nearest);

                if (!nearest) {
                    setStatus("error");
                    setErrorMessage(
                        "현재 경로의 역 좌표를 찾지 못해 위치를 갱신할 수 없습니다.",
                    );
                    return;
                }

                setStatus("active");
                setErrorMessage(null);

                const now = Date.now();
                const lastSync = lastSyncRef.current;
                const stationChanged =
                    lastSync?.placeId !== nearest.station.id;
                const syncIntervalPassed =
                    !lastSync ||
                    now - lastSync.syncedAt >= LOCATION_SYNC_INTERVAL_MS;

                if (
                    syncInFlightRef.current ||
                    (!stationChanged && !syncIntervalPassed)
                ) {
                    return;
                }

                syncInFlightRef.current = true;
                void locationApi
                    .updateShipperLocation({
                        latitude: currentPosition.latitude,
                        longitude: currentPosition.longitude,
                        placeId: nearest.station.id,
                    })
                    .then((syncedLocation) => {
                        if (!isActive) {
                            return;
                        }

                        lastSyncRef.current = {
                            placeId: nearest.station.id,
                            syncedAt: Date.now(),
                        };
                        setLastSyncedLocation(syncedLocation);
                    })
                    .catch((error: unknown) => {
                        if (!isActive) {
                            return;
                        }

                        setErrorMessage(
                            error instanceof Error
                                ? error.message
                                : "현재 위치를 서버에 전송하지 못했습니다.",
                        );
                    })
                    .finally(() => {
                        syncInFlightRef.current = false;
                    });
            },
            (error) => {
                if (!isActive) {
                    return;
                }

                setStatus(
                    error.code === error.PERMISSION_DENIED
                        ? "denied"
                        : "error",
                );
                setErrorMessage(getGeolocationErrorMessage(error));
            },
            {
                enableHighAccuracy: true,
                maximumAge: 10_000,
                timeout: 20_000,
            },
        );

        return () => {
            isActive = false;
            navigator.geolocation.clearWatch(watchId);
            syncInFlightRef.current = false;
        };
    }, [enabled, stations]);

    return {
        status,
        position,
        nearestStation,
        lastSyncedLocation,
        errorMessage,
    };
}
