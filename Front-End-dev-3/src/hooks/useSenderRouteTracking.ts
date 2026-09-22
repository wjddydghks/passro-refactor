import { useEffect, useMemo, useState } from "react";
import { locationApi, type LiveLocation } from "../apis/locationApi";
import type { SubwayStationItem } from "../apis/subwayApi";
import { ApiError } from "../types/api";
import { findNearestRouteStation } from "../utils/subwayLocation";
import type { LocationTrackingStatus } from "./useShipperRouteTracking";

const LOCATION_POLL_INTERVAL_MS = 15_000;

interface UseSenderRouteTrackingOptions {
    deliveryId: number;
    enabled: boolean;
    stations: SubwayStationItem[];
}

function getLocationErrorMessage(error: unknown) {
    if (error instanceof ApiError) {
        switch (error.code) {
            case "SHIPPER_LOCATION404_1":
                return "전달자가 아직 위치 공유를 시작하지 않았거나 마지막 위치 갱신 후 2분이 지났습니다.";
            case "SHIPPER_LOCATION400_1":
                return "전달 중 상태에서만 전달자 위치를 확인할 수 있습니다.";
        }

        return error.message;
    }

    return error instanceof Error
        ? error.message
        : "전달자 위치를 불러오지 못했습니다.";
}

export function useSenderRouteTracking({
    deliveryId,
    enabled,
    stations,
}: UseSenderRouteTrackingOptions) {
    const [location, setLocation] = useState<LiveLocation | null>(null);
    const [status, setStatus] =
        useState<LocationTrackingStatus>("idle");
    const [errorMessage, setErrorMessage] = useState<string | null>(null);

    useEffect(() => {
        if (!enabled) {
            setStatus("idle");
            setLocation(null);
            setErrorMessage(null);
            return;
        }

        let isActive = true;
        let timerId: number | null = null;
        setStatus("locating");
        setErrorMessage(null);

        const pollLocation = async () => {
            try {
                const nextLocation =
                    await locationApi.getShipperLocation(deliveryId);

                if (!isActive) {
                    return;
                }

                setLocation(nextLocation);
                setStatus("active");
                setErrorMessage(null);
            } catch (error) {
                if (!isActive) {
                    return;
                }

                setStatus("error");
                setErrorMessage(getLocationErrorMessage(error));
            } finally {
                if (isActive) {
                    timerId = window.setTimeout(
                        pollLocation,
                        LOCATION_POLL_INTERVAL_MS,
                    );
                }
            }
        };

        void pollLocation();

        return () => {
            isActive = false;
            if (timerId !== null) {
                window.clearTimeout(timerId);
            }
        };
    }, [deliveryId, enabled]);

    const nearestStation = useMemo(
        () =>
            location
                ? findNearestRouteStation(location, stations)
                : null,
        [location, stations],
    );

    return {
        location,
        status,
        errorMessage,
        nearestStation,
    };
}
