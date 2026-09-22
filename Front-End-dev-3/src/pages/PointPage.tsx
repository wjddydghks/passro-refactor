import { useCallback, useEffect, useMemo, useState } from "react";
import PageHeader from "../components/common/PageHeader";
import { PointFilterButton } from "../components/points/PointFilterButton";
import { PointList } from "../components/points/PointList";
import { TotalPoint } from "../components/points/TotalPoint";
import {
    POINT_FILTER,
    PointFilter,
    PointFilterLabel,
    PointIncrementReason,
    PointLog,
} from "../types/point";
import { useNavigate } from "react-router-dom";
import { pointApi } from "../apis";
import { useApiRequest } from "../hooks/useApiRequest";

export default function PointPage() {
    const navigate = useNavigate();
    const [selected, setSelected] = useState<PointFilterLabel>("전체");

    const loadPoint = useCallback(() => pointApi.getHistory(), []);

    const pointRequest = useApiRequest(loadPoint);

    useEffect(() => {
        void pointRequest.execute().catch(() => undefined);
    }, [pointRequest.execute]);

    const pointItems = useMemo(() => {
        return (pointRequest.data?.pointLogs ?? []).map((log) => ({
            id: log.pointLogId,
            name: getPointLogName(log),
            date: formatPointDate(log.createdAt),
            amount: Math.abs(log.deltaPoint),
            type: getPointFilterType(log.incrementReason),
        }));
    }, [pointRequest.data]);

    const currentFilter: PointFilter | null =
        Object.values(POINT_FILTER).find((c) => c.label === selected)?.code ??
        null;

    const filteredItems =
        currentFilter === null
            ? pointItems
            : pointItems.filter((item) => item.type === currentFilter);

    const totalPoint = pointRequest.data?.currentPoint;
    const isPointLoading =
        pointRequest.data === null && pointRequest.error === null;

    return (
        <div className="page-container flex flex-col h-full min-h-0 overflow-hidden">
            <PageHeader title="포인트" onBack={() => navigate(-1)} />
            {pointRequest.error && !pointRequest.data ? (
                <div className="flex min-h-[360px] flex-col items-center justify-center gap-4 px-6 text-center">
                    <p
                        className="text-sm font-medium text-red-500"
                        role="alert"
                    >
                        {pointRequest.error.message ||
                            "포인트 내역을 불러오지 못했습니다."}
                    </p>
                    <button
                        type="button"
                        onClick={() =>
                            void pointRequest.execute().catch(() => undefined)
                        }
                        className="rounded-lg bg-purple-600 px-4 py-2 text-sm font-semibold text-white"
                    >
                        다시 시도
                    </button>
                </div>
            ) : (
                <>
                    <TotalPoint total={totalPoint} isLoading={isPointLoading} />
                    <PointFilterButton
                        selected={selected}
                        onSelect={setSelected}
                    />
                    <div className="my-5 font-bold text-gray-800">
                        적립 내역
                    </div>
                    <div className="scrollbar-hidden min-h-0 flex-1 overflow-y-auto overscroll-contain">
                        <PointList items={filteredItems} />
                    </div>
                </>
            )}
        </div>
    );
}

function getPointLogName(log: PointLog) {
    return (
        log.delivery?.name?.trim() ||
        log.market?.name?.trim() ||
        log.incrementReasonMemo?.trim() ||
        "포인트 내역"
    );
}

function formatPointDate(createdAt: string) {
    const date = new Date(createdAt);

    const year = String(date.getFullYear()).slice(2);
    const month = String(date.getMonth() + 1).padStart(2, "0");
    const day = String(date.getDate()).padStart(2, "0");

    return `${year}.${month}.${day}`;
}

function getPointFilterType(reason: PointIncrementReason): PointFilter {
    switch (reason) {
        case "DELIVERY_REFUND":
        case "DELIVERY_SETTLEMENT":
            return "SAVING";
        case "DELIVERY_PAYMENT":
        case "MARKET_PURCHASE":
            return "USE";
    }
}
