import {
    BackendDeliveryLogInfo,
    BackendDeliveryPartyInfo,
    BackendDeliveryState,
    BackendPlace,
} from "../backend";

export type ShipperDeliveryListItem = {
    id: number;
    name: string;
    senderInfo: BackendDeliveryPartyInfo | null;
    shipperInfo: BackendDeliveryPartyInfo | null;
    originPlace: BackendPlace;
    destPlace: BackendPlace;
    deliveryState: BackendDeliveryState;
    memo?: string;
    createdAt: string;
    estimatedTimeMinutes: number | null;
};

export type ShipperDeliveryDetail = {
    id: number;
    name: string;
    price: number;
    size: "S" | "M" | "L";
    basePoint: number;
    distancePoint: number;
    weightPoint: number;
    totalPoint: number;
    senderInfo: BackendDeliveryPartyInfo | null;
    shipperInfo: BackendDeliveryPartyInfo | null;
    originPlace: BackendPlace;
    destPlace: BackendPlace;
    deliveryState: BackendDeliveryState;
    memo?: string;
    deliveryTimeLine: BackendDeliveryLogInfo[];
};

export type DeliveryStatusUpdateRequest = {
    imageKey?: string;
};

export type DeliveryLocation = {
    latitude: number;
    longitude: number;
    placeId: number;
};
