import type { BackendDeliveryState } from "../types/backend";

const DELIVERY_STATUS_LABELS: Record<BackendDeliveryState, string> = {
    WAIT: "매칭 대기",
    MATCHED: "픽업 대기",
    DELIVERING: "전달중",
    CONFIRM_REQUESTED: "완료 확인 대기",
    DELIVERED: "전달완료",
    CANCEL: "취소",
};

export function getDeliveryStatusLabel(status: BackendDeliveryState) {
    return DELIVERY_STATUS_LABELS[status];
}
