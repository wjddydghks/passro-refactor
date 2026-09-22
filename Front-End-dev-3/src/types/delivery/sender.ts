import {
    BackendDeliveryLogInfo,
    BackendDeliveryPartyInfo,
    BackendDeliveryState,
    BackendPlace,
} from "../backend";

export type SenderDeliveryListItem = {
    deliveryId: number;
    name: string;
    originPlace: BackendPlace;
    destPlace: BackendPlace;
    status: BackendDeliveryState;
    createdAt: string;
};

export type CreateDeliveryRequest = {
    sourceStationId: number;
    destinationStationId: number;
    name: string;
    price: number;
    size: "S" | "M" | "L";
    picture?: string;
    memo?: string;
};

export type SenderDeliveryDetail = {
    id: number;
    name: string;
    originPlace: BackendPlace;
    destPlace: BackendPlace;
    status: BackendDeliveryState;
    shipperInfo: BackendDeliveryPartyInfo | null;
    deliveryTimeLine: BackendDeliveryLogInfo[];
};

export type DeliveryStatusUpdateRequest = {
    imageKey?: string;
};

export type DeliveryPaymentRequest = {
    sourceStationId: number;
    destinationStationId: number;
    size: "S" | "M" | "L";
};

export type DeliveryPayment = {
    id: number;
    basePoint: number;
    distancePoint: number;
    weightPoint: number;
    totalPoint: number;
};

export type CreateReview = {
    deliveryId: number;
    rating: number;
    content?: string;
};

export type ReviewRequest = {
    averageRating: number;
};
