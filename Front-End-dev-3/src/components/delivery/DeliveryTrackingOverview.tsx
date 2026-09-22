import { useEffect, useState } from "react";
import type { BackendDeliveryState, BackendPlace } from "../../types/backend";
import {
    formatTrackingDateTime,
    formatTrackingTime,
    getDeliveryElapsedMinutes,
    type DeliveryRouteProgress,
} from "../../utils/deliveryTracking";
import { getDeliveryStatusLabel } from "../../utils/deliveryStatus";

interface DeliveryTrackingOverviewProps {
    itemName: string | null;
    status: BackendDeliveryState;
    originPlace: BackendPlace;
    destinationPlace: BackendPlace;
    routeProgress: DeliveryRouteProgress;
    completedAt?: string | null;
    lastLocationUpdatedAt?: string | null;
    estimatedTimeMinutes?: number | null;
    timeMode?: "estimated" | "elapsed";
    deliveryStartedAt?: string | null;
    deliveryHandedOffAt?: string | null;
    variant?: "default" | "figma";
}

function withStationSuffix(stationName: string) {
    return stationName.endsWith("역") ? stationName : `${stationName}역`;
}

function getTrackingStep(status: BackendDeliveryState) {
    if (status === "DELIVERED") return 2;
    if (status === "DELIVERING" || status === "CONFIRM_REQUESTED") return 1;
    return 0;
}

export function DeliveryTrackingProgress({
    status,
}: {
    status: BackendDeliveryState;
}) {
    const currentStep = getTrackingStep(status);
    const steps = ["물건 픽업", "전달중", "전달 완료"];

    return (
        <div className="relative mx-[5px] grid grid-cols-[60px_1fr_60px] items-start">
            <span
                className={`absolute left-[44px] right-[calc(50%+14px)] top-2 h-0.5 ${currentStep >= 1 ? "bg-gray-800" : "bg-gray-200"
                    }`}
                aria-hidden="true"
            />
            <span
                className={`absolute left-[calc(50%+14px)] right-[44px] top-2 h-0.5 ${currentStep >= 2 ? "bg-gray-800" : "bg-gray-200"
                    }`}
                aria-hidden="true"
            />
            {steps.map((label, index) => {
                const isActive = index === currentStep;
                const isReached = index <= currentStep;

                return (
                    <div
                        key={label}
                        className="relative flex flex-col items-center gap-2"
                    >
                        <span
                            className={`relative z-10 flex h-[17px] w-[17px] items-center justify-center rounded-full ${isActive
                                ? "bg-purple-100"
                                : isReached
                                    ? "bg-gray-800"
                                    : "bg-gray-200"
                                }`}
                            aria-hidden="true"
                        >
                            {isActive ? (
                                <span className="h-[7px] w-[7px] rounded-full bg-purple-500" />
                            ) : null}
                        </span>
                        <span
                            className={`relative z-10 whitespace-nowrap bg-white px-1 text-[13px] font-medium leading-[22px] ${isActive ? "text-purple-600" : "text-gray-600"
                                }`}
                        >
                            {label}
                        </span>
                    </div>
                );
            })}
        </div>
    );
}

function getCompactTimeLabel(timeLabel: string) {
    if (
        timeLabel === "정보 없음" ||
        timeLabel === "아직 시작되지 않음" ||
        timeLabel === "완료 시각 정보 없음"
    ) {
        return "경로 확인 중";
    } else if (
        timeLabel === "완료된 배송"
    ) {
        return "";
    }

    return `약 ${timeLabel.replace(/\s*(경과|소요)$/u, "")}`;
}

interface InformationCardProps {
    label: string;
    value: string;
    description?: string;
    accent?: boolean;
}

function InformationCard({
    label,
    value,
    description,
    accent = false,
}: InformationCardProps) {
    return (
        <div
            className={`rounded-2xl border px-4 py-4 ${accent
                ? "border-purple-200 bg-purple-50"
                : "border-gray-100 bg-white"
                }`}
        >
            <p className="text-[11px] font-bold text-gray-500">{label}</p>
            <p
                className={`mt-1.5 break-keep text-sm font-bold leading-snug ${accent ? "text-purple-800" : "text-gray-900"
                    }`}
                title={value}
            >
                {value}
            </p>
            {description ? (
                <p className="mt-1 break-keep text-[10px] font-medium leading-snug text-gray-400">
                    {description}
                </p>
            ) : null}
        </div>
    );
}

export function DeliveryTrackingOverview({
    itemName,
    status,
    originPlace,
    destinationPlace,
    routeProgress,
    completedAt,
    lastLocationUpdatedAt,
    estimatedTimeMinutes,
    timeMode = "estimated",
    deliveryStartedAt,
    deliveryHandedOffAt,
    variant = "default",
}: DeliveryTrackingOverviewProps) {
    const [currentTime, setCurrentTime] = useState(() => Date.now());
    const isCompleted = status === "DELIVERED";
    const isConfirmationPending = status === "CONFIRM_REQUESTED";
    const showsElapsedTime = !isCompleted && timeMode === "elapsed";

    useEffect(() => {
        if (!showsElapsedTime || !deliveryStartedAt || deliveryHandedOffAt) {
            return;
        }

        const timer = window.setInterval(
            () => setCurrentTime(Date.now()),
            30_000,
        );

        return () => window.clearInterval(timer);
    }, [deliveryHandedOffAt, deliveryStartedAt, showsElapsedTime]);

    const elapsedMinutes = getDeliveryElapsedMinutes(
        deliveryStartedAt,
        deliveryHandedOffAt,
        currentTime,
    );
    const locationUpdatedLabel = formatTrackingDateTime(lastLocationUpdatedAt);
    const currentStationLabel = isCompleted
        ? destinationPlace.subwayStationName
        : isConfirmationPending
            ? "위치 조회 불가"
            : routeProgress.currentStation
                ? routeProgress.currentStation.stationName
                : "위치 정보 없음";
    const currentStationDescription = isCompleted
        ? destinationPlace.subwayRouteName
        : isConfirmationPending
            ? "DELIVERING 상태에서만 위치 API 제공"
            : routeProgress.currentStation
                ? [
                    routeProgress.currentStation.routeName,
                    locationUpdatedLabel ? `${locationUpdatedLabel} 갱신` : null,
                ]
                    .filter(Boolean)
                    .join(" · ")
                : undefined;
    const nextStationLabel = isCompleted
        ? "전달 완료"
        : isConfirmationPending
            ? "이동 완료 · 승인 대기"
            : routeProgress.nextStation
                ? routeProgress.nextStation.stationName
                : routeProgress.currentStation
                    ? "도착역"
                    : "추적 시작 전";
    const nextStationDescription = routeProgress.nextStation?.routeName;
    const timeLabel = isCompleted
        ? ("완료된 배송")
        : showsElapsedTime
            ? elapsedMinutes == null
                ? "아직 시작되지 않음"
                : elapsedMinutes < 1
                    ? deliveryHandedOffAt
                        ? "1분 미만 소요"
                        : "1분 미만 경과"
                    : elapsedMinutes >= 60
                        ? `${Math.floor(elapsedMinutes / 60)}시간 ${elapsedMinutes % 60}분 ${deliveryHandedOffAt ? "소요" : "경과"}`
                        : `${elapsedMinutes}분 ${deliveryHandedOffAt ? "소요" : "경과"}`
            : estimatedTimeMinutes != null
                ? `${estimatedTimeMinutes}분`
                : "정보 없음";
    const timeDescription = isCompleted
        ? undefined
        : showsElapsedTime
            ? deliveryStartedAt
                ? deliveryHandedOffAt
                    ? "물품 인수부터 전달 완료 요청까지"
                    : `${formatTrackingTime(deliveryStartedAt) ?? "처리 시각 미상"} 전달 시작`
                : "물품 인수 후 자동으로 계산됩니다"
            : estimatedTimeMinutes != null
                ? "현재 역에서 도착역까지 예상 소요 시간"
                : "전달자 위치가 갱신되면 표시됩니다";

    if (variant === "figma") {
        return (
            <section className="pt-7">
                <DeliveryTrackingProgress status={status} />

                <div className="mt-6 grid grid-cols-[1fr_64px_1fr] items-center rounded-xl bg-gray-50 px-7 py-5 text-center">
                    <div className="min-w-0">
                        <p className="truncate text-lg font-bold text-gray-900">
                            {withStationSuffix(originPlace.subwayStationName)}
                        </p>
                        <p className="mt-2 text-xs font-medium text-gray-400">
                            출발지
                        </p>
                    </div>
                    <div>
                        <p
                            className="text-[15px] text-gray-500"
                            aria-hidden="true"
                        >
                            →
                        </p>
                        <p className="mt-1 truncate text-xs font-medium text-gray-400">
                            {getCompactTimeLabel(timeLabel)}
                        </p>
                    </div>
                    <div className="min-w-0">
                        <p className="truncate text-lg font-bold text-gray-900">
                            {withStationSuffix(
                                destinationPlace.subwayStationName,
                            )}
                        </p>
                        <p className="mt-2 text-xs font-medium text-gray-400">
                            도착지
                        </p>
                    </div>
                </div>
            </section>
        );
    }

    return (
        <section
            className={`mt-5 overflow-hidden rounded-3xl px-5 py-5 text-white shadow-sm ${isCompleted
                ? "bg-gradient-to-br from-emerald-600 to-teal-700"
                : "bg-gradient-to-br from-purple-700 via-purple-600 to-indigo-600"
                }`}
        >
            <div className="flex flex-col gap-5">
                <div className="min-w-0">
                    <span className="inline-flex rounded-full bg-white/15 px-3 py-1 text-xs font-bold backdrop-blur-sm">
                        {getDeliveryStatusLabel(status)}
                    </span>
                    <h1 className="mt-3 truncate text-xl font-bold">
                        {isCompleted
                            ? "전달이 완료되었습니다"
                            : (itemName ?? "이름 없는 물품")}
                    </h1>
                    <p className="mt-1 text-sm font-medium text-white/75">
                        {isCompleted
                            ? `${itemName ?? "물품"}의 최종 전달 정보입니다.`
                            : "현재 위치와 남은 전달 경로를 확인하세요."}
                    </p>
                </div>

                <div className="grid shrink-0 grid-cols-[1fr_auto_1fr] items-center gap-3 rounded-2xl bg-black/10 px-4 py-3">
                    <div className="min-w-0">
                        <p className="text-[10px] font-bold text-white/60">
                            출발지
                        </p>
                        <p className="mt-1 truncate text-sm font-bold">
                            {originPlace.subwayStationName}
                        </p>
                    </div>
                    <span className="text-lg text-white/50" aria-hidden="true">
                        →
                    </span>
                    <div className="min-w-0 text-right">
                        <p className="text-[10px] font-bold text-white/60">
                            도착지
                        </p>
                        <p className="mt-1 truncate text-sm font-bold">
                            {destinationPlace.subwayStationName}
                        </p>
                    </div>
                </div>
            </div>

            <div className="mt-5 grid grid-cols-2 gap-2">
                <InformationCard
                    label={isCompleted ? "최종 도착역" : "현재 전달자 위치"}
                    value={currentStationLabel}
                    description={currentStationDescription}
                    accent
                />
                <InformationCard
                    label={isCompleted ? "전달 결과" : "다음 이동 예정 역"}
                    value={nextStationLabel}
                    description={nextStationDescription}
                />
                <InformationCard
                    label={
                        isCompleted
                            ? "완료 시각"
                            : showsElapsedTime
                                ? "전달 경과 시간"
                                : "도착 예상 시간"
                    }
                    value={timeLabel}
                    description={timeDescription}
                />
                <InformationCard
                    label="현재 전달 상태"
                    value={getDeliveryStatusLabel(status)}
                />
            </div>

            <div className="mt-5 rounded-2xl bg-black/10 px-4 py-3">
                <div className="flex items-center justify-between gap-4 text-xs font-bold">
                    <span>전체 전달 경로 진행률</span>
                    <span>
                        {routeProgress.totalStationCount > 0
                            ? `${routeProgress.completedStationCount}/${routeProgress.totalStationCount}역 · `
                            : isCompleted
                                ? "이동 완료 · "
                                : "경로 확인 중 · "}
                        {routeProgress.percentage}%
                    </span>
                </div>
                <div
                    className="mt-2.5 h-2 overflow-hidden rounded-full bg-white/20"
                    role="progressbar"
                    aria-label="전체 전달 경로 진행률"
                    aria-valuemin={0}
                    aria-valuemax={100}
                    aria-valuenow={routeProgress.percentage}
                >
                    <div
                        className="h-full rounded-full bg-white transition-[width] duration-700"
                        style={{ width: `${routeProgress.percentage}%` }}
                    />
                </div>
            </div>
        </section>
    );
}
