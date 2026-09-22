import { apiRequest } from "./client";
import { API_ENDPOINTS } from "./endpoints";

export interface LiveLocation {
    latitude: number;
    longitude: number;
    placeId: number;
    updatedAt: string;
    estimatedTimeMinutes: number | null;
}

export interface ShipperLocationUpdateRequest {
    latitude: number;
    longitude: number;
    placeId: number;
}

export const locationApi = {
    updateShipperLocation(request: ShipperLocationUpdateRequest) {
        return apiRequest<LiveLocation>({
            method: "PUT",
            url: API_ENDPOINTS.shipper.location,
            data: request,
        });
    },

    getShipperLocation(deliveryId: number) {
        return apiRequest<LiveLocation>({
            method: "GET",
            url: API_ENDPOINTS.sender.shipperLocation(deliveryId),
        });
    },
};
