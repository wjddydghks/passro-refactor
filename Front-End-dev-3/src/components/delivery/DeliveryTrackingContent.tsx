import type { ReactNode } from "react";
import type { SubwayPathItem } from "../../apis/subwayApi";
import type { LocationTrackingStatus } from "../../hooks/useShipperRouteTracking";
import type {
    BackendDeliveryLogInfo,
    BackendDeliveryPartyInfo,
    BackendDeliveryState,
    BackendPlace,
} from "../../types/backend";
import {
    getDeliveryCompletedAt,
    getDeliveryHandedOffAt,
    getDeliveryRouteProgress,
    getDeliveryStartedAt,
} from "../../utils/deliveryTracking";
import type { GeographicCoordinates } from "../../utils/subwayLocation";
import { DeliveryPersonCard } from "./DeliveryPersonCard";
import { DeliveryTimeline } from "./DeliveryTimeline";
import { DeliveryTrackingOverview } from "./DeliveryTrackingOverview";
import { SubwayRouteTracker } from "./SubwayRouteTracker";

interface DeliveryTrackingContentProps {
    itemName: string | null;
    status: BackendDeliveryState;
    originPlace: BackendPlace;
    destinationPlace: BackendPlace;
    logs: BackendDeliveryLogInfo[];
    route: SubwayPathItem | null;
    isRouteLoading: boolean;
    routeError?: string | null;
    currentPlaceId?: number;
    currentCoordinates?: GeographicCoordinates;
    currentDistanceMeters?: number;
    gpsAccuracyMeters?: number;
    trackingStatus: LocationTrackingStatus;
    trackingError?: string | null;
    lastLocationUpdatedAt?: string | null;
    estimatedTimeMinutes?: number | null;
    overviewTimeMode?: "estimated" | "elapsed";
    trackingStatusMessage?: string;
    partyTitle: string;
    party: BackendDeliveryPartyInfo | null;
    partyEmptyMessage: string;
    onPartyMessageClick?: () => void;
    supplementaryContent?: ReactNode;
    onReview?: () => void;
    variant?: "default" | "figma";
}

function formatPartyPlace(place: BackendPlace | null) {
    return place ? `${place.subwayStationName}역` : null;
}

function RouteLoadingSkeleton({ compact = false }: { compact?: boolean }) {
    return (
        <div
            className={`${compact ? "h-[280px] rounded-[10px]" : "h-[460px] rounded-3xl"} animate-pulse bg-gray-100`}
            aria-label="지하철 경로를 불러오는 중"
            aria-busy="true"
        />
    );
}

export function DeliveryTrackingContent({
    itemName,
    status,
    originPlace,
    destinationPlace,
    logs,
    route,
    isRouteLoading,
    routeError,
    currentPlaceId,
    currentCoordinates,
    currentDistanceMeters,
    gpsAccuracyMeters,
    trackingStatus,
    trackingError,
    lastLocationUpdatedAt,
    estimatedTimeMinutes,
    overviewTimeMode = "estimated",
    trackingStatusMessage,
    partyTitle,
    party,
    partyEmptyMessage,
    onPartyMessageClick,
    supplementaryContent,
    onReview,
    variant = "default",
}: DeliveryTrackingContentProps) {
    const isCompleted = status === "DELIVERED";
    const routeProgress = getDeliveryRouteProgress(
        route?.stations ?? [],
        currentPlaceId,
        status,
    );
    const completedAt = getDeliveryCompletedAt(logs);
    const deliveryStartedAt = getDeliveryStartedAt(logs);
    const deliveryHandedOffAt = getDeliveryHandedOffAt(logs);
    const liveStationLabel =
        currentPlaceId !== undefined && routeProgress.currentStation
            ? `${routeProgress.currentStation.stationName} · ${routeProgress.currentStation.routeName}`
            : null;
    const partySection = (
        <section
            className={
                variant === "figma"
                    ? ""
                    : "rounded-3xl border border-gray-100 bg-white px-5 py-5 shadow-sm"
            }
        >
            <h2
                className={`${variant === "figma" ? "mb-3 text-[17px] leading-[22px]" : "mb-3 text-base"} font-bold text-gray-900`}
            >
                {partyTitle}
            </h2>
            <DeliveryPersonCard
                name={party?.nickname ?? null}
                picture={party?.picture}
                place={
                    isCompleted
                        ? null
                        : `${formatPartyPlace(party?.originPlace ?? null)} → ${formatPartyPlace(party?.destPlace ?? null)}`
                }
                emptyMessage={partyEmptyMessage}
                actionLabel={
                    isCompleted ? "이번 전달은 어떠셨나요?" : undefined
                }
                onAction={isCompleted ? onReview : undefined}
                onMessageClick={party ? onPartyMessageClick : undefined}
            />
        </section>
    );

    const routeSection = isRouteLoading ? (
        <RouteLoadingSkeleton compact={variant === "figma"} />
    ) : route ? (
        <SubwayRouteTracker
            stations={route.stations}
            currentPlaceId={currentPlaceId}
            currentCoordinates={currentCoordinates}
            currentDistanceMeters={currentDistanceMeters}
            gpsAccuracyMeters={gpsAccuracyMeters}
            trackingStatus={trackingStatus}
            trackingError={trackingError}
            transferCount={route.transferCount}
            lastUpdatedAt={lastLocationUpdatedAt}
            statusMessage={trackingStatusMessage}
            variant={variant}
        />
    ) : (
        <p
            className="rounded-xl border border-rose-200 bg-rose-50 px-5 py-8 text-center text-sm font-medium text-rose-700"
            role="alert"
        >
            {routeError ?? "지하철 경로를 불러오지 못했습니다."}
        </p>
    );

    if (variant === "figma") {
        return (
            <>
                <DeliveryTrackingOverview
                    itemName={itemName}
                    status={status}
                    originPlace={originPlace}
                    destinationPlace={destinationPlace}
                    routeProgress={routeProgress}
                    completedAt={completedAt}
                    lastLocationUpdatedAt={lastLocationUpdatedAt}
                    estimatedTimeMinutes={estimatedTimeMinutes}
                    timeMode={overviewTimeMode}
                    deliveryStartedAt={deliveryStartedAt}
                    deliveryHandedOffAt={deliveryHandedOffAt}
                    variant="figma"
                />
                {!isCompleted ? (
                    <div className="mt-8">{routeSection}</div>
                ) : null}
                <div className="mt-8">
                    <DeliveryTimeline
                        status={status}
                        logs={logs}
                        originPlace={originPlace}
                        destinationPlace={destinationPlace}
                        currentStationLabel={liveStationLabel}
                        variant="figma"
                    />
                </div>
                <div className="mt-10">{partySection}</div>
                {supplementaryContent ? (
                    <div className="mt-5">{supplementaryContent}</div>
                ) : null}
            </>
        );
    }

    return (
        <>
            <DeliveryTrackingOverview
                itemName={itemName}
                status={status}
                originPlace={originPlace}
                destinationPlace={destinationPlace}
                routeProgress={routeProgress}
                completedAt={completedAt}
                lastLocationUpdatedAt={lastLocationUpdatedAt}
                estimatedTimeMinutes={estimatedTimeMinutes}
                timeMode={overviewTimeMode}
                deliveryStartedAt={deliveryStartedAt}
                deliveryHandedOffAt={deliveryHandedOffAt}
            />

            {isCompleted ? (
                <div className="mt-6 grid gap-6">
                    <DeliveryTimeline
                        status={status}
                        logs={logs}
                        originPlace={originPlace}
                        destinationPlace={destinationPlace}
                    />
                    {partySection}
                </div>
            ) : (
                <>
                    <div className="mt-6 grid gap-6">
                        <div>{routeSection}</div>

                        <aside className="flex flex-col gap-5">
                            {partySection}
                            {supplementaryContent}
                        </aside>
                    </div>

                    <div className="mt-6">
                        <DeliveryTimeline
                            status={status}
                            logs={logs}
                            originPlace={originPlace}
                            destinationPlace={destinationPlace}
                            currentStationLabel={liveStationLabel}
                        />
                    </div>
                </>
            )}
        </>
    );
}
