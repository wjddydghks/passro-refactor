export type BackendDeliveryState =
    | "WAIT"
    | "MATCHED"
    | "DELIVERING"
    | "CONFIRM_REQUESTED"
    | "DELIVERED"
    | "CANCEL";

export type BackendDeliveryLogType =
    | "SEND_REQUEST"
    | "MATCHED"
    | "PICKED_UP"
    | "DELIVERED"
    | "DONE"
    | "CANCELED";

export interface BackendPlace {
    createdAt: string;
    updatedAt: string;
    id: number;
    subwayRouteName: string;
    subwayStationName: string;
    lotitude: number;
    longtitud: number;
}

export interface BackendDeliveryPartyInfo {
    name: string;
    nickname: string;
    picture: string | null;
    originPlace: BackendPlace | null;
    destPlace: BackendPlace | null;
}

export interface BackendDeliveryLogInfo {
    id: number;
    type: BackendDeliveryLogType;
    image: string | null;
    createdAt: string;
}
