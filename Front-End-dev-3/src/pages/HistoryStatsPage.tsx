import { useNavigate } from "react-router-dom";
import PageHeader from "../components/common/PageHeader";
import { DeliveryFilterButton } from "../components/delivery/history/DeliveryFilterButton";
import { DeliveryList } from "../components/delivery/history/DeliveryList";
import { useCallback, useEffect, useMemo, useState } from "react";
import {
    DELIVERY_FILTER,
    DeliveryFilter,
    DeliveryFilterLabel,
    DeliveryStatus,
} from "../types/delivery/delivery";
import { senderDeliveryApi, shipperDeliveryApi } from "../apis";
import { useApiRequest } from "../hooks/useApiRequest";
import { ShipperDeliveryListItem } from "../types/delivery/shipper";
import { BackendDeliveryState } from "../types/backend";
import type { UserRole } from "../types/user";
import { SenderDeliveryListItem } from "../types/delivery/sender";

interface DeliveryItem {
    id: number;
    name: string;
    start?: string;
    end?: string;
    date?: string;
    status: DeliveryStatus;
    role: UserRole;
}

function getDeliveryStatus(state: BackendDeliveryState): DeliveryStatus {
    switch (state) {
        case "WAIT":
        case "MATCHED":
            return "WAITING_PICKUP";
        case "DELIVERING":
        case "CONFIRM_REQUESTED":
            return "DELIVERING";
        case "DELIVERED":
        case "CANCEL":
            return "COMPLETED";
    }
}

function formatDeliveryDate(createdAt: string) {
    const date = new Date(createdAt);

    const year = String(date.getFullYear()).slice(2);
    const month = String(date.getMonth() + 1).padStart(2, "0");
    const day = String(date.getDate()).padStart(2, "0");

    return `${year}.${month}.${day}`;
}

function toShipperDeliveryItem(
    delivery: ShipperDeliveryListItem,
): DeliveryItem {
    return {
        id: delivery.id,
        name: delivery.name,
        start: delivery.originPlace.subwayStationName,
        end: delivery.destPlace.subwayStationName,
        date: formatDeliveryDate(delivery.createdAt),
        status: getDeliveryStatus(delivery.deliveryState),
        role: "shipper",
    };
}

function toSenderDeliveryItem(delivery: SenderDeliveryListItem): DeliveryItem {
    return {
        id: delivery.deliveryId,
        name: delivery.name,
        start: delivery.originPlace.subwayStationName,
        end: delivery.destPlace.subwayStationName,
        date: formatDeliveryDate(delivery.createdAt),
        status: getDeliveryStatus(delivery.status),
        role: "sender",
    };
}

export function HistoryStatsPage() {
    const navigate = useNavigate();

    const [selected, setSelected] = useState<DeliveryFilterLabel>("전체");
    const [role, setRole] = useState<UserRole>("shipper");
    const isShipper = role === "shipper";

    const loadHistories = useCallback(
        () =>
            Promise.all([
                senderDeliveryApi.getDeliveryList(),
                shipperDeliveryApi.getDeliveryList(),
            ]),
        [],
    );
    const deliveryRequest = useApiRequest(loadHistories);

    useEffect(() => {
        void deliveryRequest.execute().catch(() => undefined);
    }, [deliveryRequest.execute]);

    const deliveryItems = useMemo(() => {
        const senderDeliveries = deliveryRequest.data?.[0] ?? [];
        const shipperDeliveries = deliveryRequest.data?.[1] ?? [];

        return [
            ...senderDeliveries.map(toSenderDeliveryItem),
            ...shipperDeliveries.map(toShipperDeliveryItem),
        ];
    }, [deliveryRequest.data]);

    const currentFilter: DeliveryFilter | null =
        Object.values(DELIVERY_FILTER).find((c) => c.label === selected)
            ?.code ?? null;
    const filteredItems = useMemo(() => {
        const roleFilteredItem = deliveryItems.filter(
            (item) => item.role === role,
        );
        if (currentFilter === null) return roleFilteredItem;
        return roleFilteredItem.filter((item) => item.status === currentFilter);
    }, [deliveryItems, role, currentFilter]);

    return (
        <div className="page-container flex h-dvh min-h-0 flex-col overflow-hidden overscroll-none">
            <PageHeader
                title="활동 내역"
                onBack={() => navigate("/mypage")}
                className="shrink-0"
            />
            {deliveryRequest.error && !deliveryRequest.data ? (
                <div className="flex min-h-[360px] flex-col items-center justify-center gap-4 px-6 text-center">
                    <p
                        className="text-sm font-medium text-red-500"
                        role="alert"
                    >
                        {deliveryRequest.error.message ||
                            "활동 내역을 불러오지 못했습니다."}
                    </p>
                    <button
                        type="button"
                        onClick={() =>
                            void deliveryRequest
                                .execute()
                                .catch(() => undefined)
                        }
                        className="rounded-lg bg-purple-600 px-4 py-2 text-sm font-semibold text-white"
                    >
                        다시 시도
                    </button>
                </div>
            ) : (
                <>
                    <div className="mt-8 flex items-center justify-between gap-3">
                        <DeliveryFilterButton
                            selected={selected}
                            onSelect={setSelected}
                        />
                        <button
                            type="button"
                            role="switch"
                            aria-checked={isShipper}
                            aria-label={`${isShipper ? "전달" : "요청"} 내역만 표시 중`}
                            onClick={() =>
                                setRole(isShipper ? "sender" : "shipper")
                            }
                            className="flex shrink-0 items-center gap-1.5 rounded-full px-2 py-2 text-[11px] font-semibold text-gray-700 transition-colors focus:outline-none focus-visible:ring-2 focus-visible:ring-purple-500 focus-visible:ring-offset-2"
                        >
                            <span
                                className={`relative h-[22px] w-[42px] rounded-full transition-colors ${
                                    isShipper ? "bg-purple-600" : "bg-gray-400"
                                }`}
                                aria-hidden="true"
                            >
                                <span
                                    className={`absolute top-0.5 h-[18px] w-[18px] rounded-full bg-white shadow-sm transition-transform ${
                                        isShipper
                                            ? "translate-x-[-18px]"
                                            : "translate-x-[0px]"
                                    }`}
                                />
                            </span>
                            <span>
                                <span
                                    className={`${isShipper ? "text-purple-800" : "text-[#AE9841]"}`}
                                >{`${isShipper ? "전달" : "요청"}`}</span>
                                <span>만 표시</span>
                            </span>
                        </button>
                    </div>
                    <DeliveryList items={filteredItems} />
                </>
            )}
        </div>
    );
}
