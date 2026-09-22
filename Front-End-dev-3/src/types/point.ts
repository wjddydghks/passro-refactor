import { BackendDeliveryState } from "./backend";

export const POINT_FILTER = {
    ALL: {
        label: "전체",
        code: null, // 또는 "ALL" (서버 정책에 맞게)
    },
    SAVING: {
        label: "적립",
        code: "SAVING",
    },
    USE: {
        label: "사용",
        code: "USE",
    },
    EXPIRE: {
        label: "소멸",
        code: "EXPIRE",
    },
} as const;

export type PointFilterKey = keyof typeof POINT_FILTER;

export type PointFilterLabel = (typeof POINT_FILTER)[PointFilterKey]["label"];

export type PointFilter = Exclude<
    (typeof POINT_FILTER)[PointFilterKey]["code"],
    null
>;

export type PointIncrementReason =
    | "DELIVERY_PAYMENT"
    | "DELIVERY_REFUND"
    | "DELIVERY_SETTLEMENT"
    | "MARKET_PURCHASE";

export type PointPlace = {
    id: number;
    subwayRouteName: string;
    subwayStationName: string;
};

export type PointDelivery = {
    id: number;
    name: string | null;
    origin: PointPlace | null;
    destination: PointPlace | null;
    status: BackendDeliveryState;
    memo: string | null;
};

export type PointMarket = {
    id: number;
    name: string;
    price: number;
};

export type PointLog = {
    pointLogId: number;
    delivery: PointDelivery | null;
    market: PointMarket | null;
    incrementReason: PointIncrementReason;
    deltaPoint: number;
    beforePoint: number;
    afterPoint: number;
    incrementReasonMemo: string | null;
    createdAt: string;
};

export type Point = {
    currentPoint: number;
    pointLogs: PointLog[];
};
