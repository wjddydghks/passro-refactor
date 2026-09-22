export type DeliveryStatus = "WAITING_PICKUP" | "DELIVERING" | "COMPLETED";

export const DELIVERY_FILTER = {
    ALL: {
        label: "전체",
        code: null, // 또는 "ALL" (서버 정책에 맞게)
    },
    DELIVERING: {
        label: "전달중",
        code: "DELIVERING",
    },
    COMPLETED: {
        label: "전달완료",
        code: "COMPLETED",
    },
} as const;

export type DeliveryFilterKey = keyof typeof DELIVERY_FILTER;

export type DeliveryFilterLabel =
    (typeof DELIVERY_FILTER)[DeliveryFilterKey]["label"];

export type DeliveryFilter = Exclude<
    (typeof DELIVERY_FILTER)[DeliveryFilterKey]["code"],
    null
>;
