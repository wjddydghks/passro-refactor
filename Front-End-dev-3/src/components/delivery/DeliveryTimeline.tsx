import type {
    BackendDeliveryLogInfo,
    BackendDeliveryLogType,
    BackendDeliveryState,
    BackendPlace,
} from "../../types/backend";
import {
    formatDeliveryPlace,
    formatTrackingDateTime,
} from "../../utils/deliveryTracking";

type TimelineState = "completed" | "current" | "pending" | "canceled";

interface DeliveryTimelineProps {
    status: BackendDeliveryState;
    logs: BackendDeliveryLogInfo[];
    originPlace: BackendPlace;
    destinationPlace: BackendPlace;
    currentStationLabel?: string | null;
    variant?: "default" | "figma";
}

interface TimelineDefinition {
    type: Exclude<BackendDeliveryLogType, "CANCELED">;
    label: string;
    description: string;
}

interface TimelineItem {
    type: BackendDeliveryLogType;
    label: string;
    description: string;
    log: BackendDeliveryLogInfo | undefined;
    state: TimelineState;
}

const TIMELINE_DEFINITIONS: TimelineDefinition[] = [
    {
        type: "SEND_REQUEST",
        label: "전달 요청",
        description: "전달 요청이 등록되었습니다.",
    },
    {
        type: "MATCHED",
        label: "전달자 매칭",
        description: "전달을 수행할 전달자가 배정됩니다.",
    },
    {
        type: "PICKED_UP",
        label: "물품 인수",
        description: "전달자가 물품을 인수하고 이동을 시작합니다.",
    },
    {
        type: "DELIVERED",
        label: "전달 완료 요청",
        description: "도착지 전달 후 발송자의 확인을 요청합니다.",
    },
    {
        type: "DONE",
        label: "전달 완료",
        description: "발송자가 최종 전달 완료를 승인합니다.",
    },
];

const CURRENT_TARGET_BY_STATUS: Partial<
    Record<BackendDeliveryState, BackendDeliveryLogType>
> = {
    WAIT: "MATCHED",
    MATCHED: "PICKED_UP",
    DELIVERING: "DELIVERED",
    CONFIRM_REQUESTED: "DONE",
    CANCEL: "CANCELED",
};

const TIME_ZONE_SUFFIX_PATTERN = /(?:Z|[+-]\d{2}:?\d{2})$/u;

function formatCompactDateTime(value: string | undefined) {
    if (!value) return null;

    const date = new Date(
        TIME_ZONE_SUFFIX_PATTERN.test(value) ? value : `${value}Z`,
    );
    if (Number.isNaN(date.getTime())) return value;

    const parts = new Intl.DateTimeFormat("ko-KR", {
        month: "2-digit",
        day: "2-digit",
        weekday: "short",
        hour: "2-digit",
        minute: "2-digit",
        hour12: false,
        timeZone: "Asia/Seoul",
    }).formatToParts(date);
    const getPart = (type: Intl.DateTimeFormatPartTypes) =>
        parts.find((part) => part.type === type)?.value ?? "";

    return `${getPart("month")}.${getPart("day")}(${getPart("weekday")}) ${getPart("hour")}:${getPart("minute")}`;
}

function getTimelineLocation(
    type: BackendDeliveryLogType,
    state: TimelineState,
    originPlace: BackendPlace,
    destinationPlace: BackendPlace,
    currentStationLabel?: string | null,
) {
    if (state === "current" && currentStationLabel) {
        return `현재 위치 · ${currentStationLabel}`;
    }

    switch (type) {
        case "SEND_REQUEST":
        case "PICKED_UP":
            return `출발지 · ${formatDeliveryPlace(originPlace)}`;
        case "DELIVERED":
        case "DONE":
            return `도착지 · ${formatDeliveryPlace(destinationPlace)}`;
        case "MATCHED":
        case "CANCELED":
            return "로그 위치 정보 미제공";
    }
}

function getStateLabel(state: TimelineState) {
    switch (state) {
        case "completed":
            return "완료";
        case "current":
            return "진행 중";
        case "pending":
            return "예정";
        case "canceled":
            return "취소";
    }
}

export function DeliveryTimeline({
    status,
    logs,
    originPlace,
    destinationPlace,
    currentStationLabel,
    variant = "default",
}: DeliveryTimelineProps) {
    const currentTarget = CURRENT_TARGET_BY_STATUS[status];
    const logsByType = new Map(logs.map((log) => [log.type, log]));
    const timelineItems: TimelineItem[] = TIMELINE_DEFINITIONS.map(
        (definition) => {
            const log = logsByType.get(definition.type);
            const state: TimelineState = log
                ? "completed"
                : currentTarget === definition.type
                  ? "current"
                  : "pending";

            return {
                ...definition,
                log,
                state,
            };
        },
    );
    const canceledLog = logsByType.get("CANCELED");

    if (status === "CANCEL" || canceledLog) {
        timelineItems.push({
            type: "CANCELED",
            label: "전달 취소",
            description: "전달 요청이 취소되었습니다.",
            log: canceledLog,
            state: canceledLog ? "canceled" : "current",
        });
    }

    if (variant === "figma") {
        return (
            <section>
                <h2 className="text-[17px] font-bold leading-[22px] text-gray-900">
                    전달 타임라인
                </h2>
                <ol className="mt-5 pl-2.5">
                    {timelineItems.map((item, index) => {
                        const isLast = index === timelineItems.length - 1;
                        const isPending = item.state === "pending";
                        const location = getTimelineLocation(
                            item.type,
                            item.state,
                            originPlace,
                            destinationPlace,
                            currentStationLabel,
                        );
                        const locationLabel = location
                            ?.replace(/^출발지 · /u, "")
                            .replace(/^도착지 · /u, "")
                            .replace(/^현재 위치 · /u, "");

                        return (
                            <li
                                key={item.type}
                                className="relative grid grid-cols-[10px_minmax(0,1fr)] gap-[15px] pb-6 last:pb-0"
                            >
                                {!isLast ? (
                                    <span
                                        className={`absolute -bottom-1.5 left-[4px] top-4 border-l-2 border-dotted ${
                                            item.state === "completed"
                                                ? "border-purple-300"
                                                : "border-gray-200"
                                        }`}
                                        aria-hidden="true"
                                    />
                                ) : null}
                                <span
                                    className={`relative z-10 mt-1.5 h-2.5 w-2.5 rounded-full ${
                                        item.state === "completed" ||
                                        item.state === "current"
                                            ? "bg-purple-500"
                                            : item.state === "canceled"
                                              ? "bg-rose-500"
                                              : "bg-gray-300"
                                    }`}
                                    aria-hidden="true"
                                />
                                <div className="min-w-0">
                                    <div className="flex items-center justify-between gap-3">
                                        <h3
                                            className={`text-[15px] font-medium leading-[22px] ${
                                                isPending
                                                    ? "text-gray-400"
                                                    : "text-gray-900"
                                            }`}
                                        >
                                            {item.label}
                                        </h3>
                                        {item.log?.createdAt ||
                                        item.state === "current" ? (
                                            <span className="shrink-0 text-xs font-semibold text-gray-500">
                                                {item.state === "current"
                                                    ? "진행중"
                                                    : formatCompactDateTime(
                                                          item.log?.createdAt,
                                                      )}
                                            </span>
                                        ) : null}
                                    </div>
                                    <p
                                        className={`mt-1 text-[13px] font-medium leading-normal ${
                                            isPending
                                                ? "text-gray-300"
                                                : "text-gray-500"
                                        }`}
                                    >
                                        {item.description}
                                    </p>
                                    {locationLabel &&
                                    locationLabel !==
                                        "로그 위치 정보 미제공" ? (
                                        <span
                                            className={`mt-2 inline-flex rounded-md bg-gray-50 px-2 py-1 text-xs font-semibold ${
                                                isPending
                                                    ? "text-gray-300"
                                                    : "text-purple-800"
                                            }`}
                                        >
                                            {locationLabel}
                                        </span>
                                    ) : null}
                                </div>
                            </li>
                        );
                    })}
                </ol>
            </section>
        );
    }

    return (
        <section className="rounded-3xl border border-gray-100 bg-white px-5 py-5 shadow-sm">
            <div>
                <h2 className="text-lg font-bold text-gray-900">
                    전달 타임라인
                </h2>
                <p className="mt-1 text-xs font-medium text-gray-500">
                    백엔드에 기록된 단계별 처리 이력입니다.
                </p>
            </div>

            <ol className="mt-6">
                {timelineItems.map((item, index) => {
                    const isLast = index === timelineItems.length - 1;
                    const formattedTime = formatTrackingDateTime(
                        item.log?.createdAt,
                    );
                    const location = getTimelineLocation(
                        item.type,
                        item.state,
                        originPlace,
                        destinationPlace,
                        currentStationLabel,
                    );

                    return (
                        <li
                            key={item.type}
                            className="relative grid grid-cols-[28px_minmax(0,1fr)] gap-3 pb-7 last:pb-0"
                        >
                            {!isLast ? (
                                <span
                                    className={`absolute bottom-0 left-[13px] top-7 w-0.5 ${
                                        item.state === "completed"
                                            ? "bg-purple-300"
                                            : "bg-gray-100"
                                    }`}
                                    aria-hidden="true"
                                />
                            ) : null}

                            <span
                                className={`relative z-10 flex h-7 w-7 items-center justify-center rounded-full border-2 text-xs font-black ${
                                    item.state === "completed"
                                        ? "border-purple-600 bg-purple-600 text-white"
                                        : item.state === "current"
                                          ? "animate-pulse border-purple-500 bg-purple-100 text-purple-700"
                                          : item.state === "canceled"
                                            ? "border-rose-500 bg-rose-500 text-white"
                                            : "border-gray-200 bg-white text-gray-300"
                                }`}
                            >
                                {item.state === "completed"
                                    ? "✓"
                                    : item.state === "canceled"
                                      ? "!"
                                      : index + 1}
                            </span>

                            <div className="min-w-0 pt-0.5">
                                <div className="flex flex-wrap items-center justify-between gap-x-3 gap-y-1">
                                    <div className="flex min-w-0 items-center gap-2">
                                        <h3
                                            className={`truncate text-sm font-bold ${
                                                item.state === "pending"
                                                    ? "text-gray-400"
                                                    : item.state === "canceled"
                                                      ? "text-rose-700"
                                                      : "text-gray-900"
                                            }`}
                                        >
                                            {item.label}
                                        </h3>
                                        <span
                                            className={`shrink-0 rounded-full px-2 py-0.5 text-[10px] font-bold ${
                                                item.state === "completed"
                                                    ? "bg-purple-50 text-purple-700"
                                                    : item.state === "current"
                                                      ? "bg-amber-50 text-amber-700"
                                                      : item.state ===
                                                          "canceled"
                                                        ? "bg-rose-50 text-rose-700"
                                                        : "bg-gray-50 text-gray-400"
                                            }`}
                                        >
                                            {getStateLabel(item.state)}
                                        </span>
                                    </div>
                                    <time className="shrink-0 text-[11px] font-semibold text-gray-500">
                                        {formattedTime ??
                                            (item.state === "current"
                                                ? "진행 중"
                                                : "처리 시각 미정")}
                                    </time>
                                </div>
                                <p
                                    className={`mt-1 text-xs font-medium ${
                                        item.state === "pending"
                                            ? "text-gray-300"
                                            : "text-gray-500"
                                    }`}
                                >
                                    {item.description}
                                </p>
                                <p
                                    className={`mt-2 inline-flex rounded-lg px-2.5 py-1 text-[11px] font-semibold ${
                                        location === "로그 위치 정보 미제공"
                                            ? "bg-amber-50 text-amber-700"
                                            : "bg-gray-50 text-gray-600"
                                    }`}
                                >
                                    {location}
                                </p>
                            </div>
                        </li>
                    );
                })}
            </ol>
        </section>
    );
}
