import { DeliveryStatusUpdateRequest } from "../../types/delivery/sender";
import {
    ShipperDeliveryListItem,
    ShipperDeliveryDetail,
    DeliveryLocation,
} from "../../types/delivery/shipper";
import { apiRequest } from "../client";
import { API_ENDPOINTS } from "../endpoints";

export const shipperDeliveryApi = {
    getMatchRequests() {
        return apiRequest<ShipperDeliveryListItem[]>({
            method: "GET",
            url: API_ENDPOINTS.shipper.matched,
        });
    },

    getDeliveryList() {
        return apiRequest<ShipperDeliveryListItem[]>({
            method: "GET",
            url: API_ENDPOINTS.shipper.root,
        });
    },

    getDeliveryDetail(deliveryId: number) {
        return apiRequest<ShipperDeliveryDetail>({
            method: "GET",
            url: API_ENDPOINTS.shipper.detail(deliveryId),
        });
    },

    accept(deliveryId: number) {
        return apiRequest<null>({
            method: "PATCH",
            url: API_ENDPOINTS.shipper.acceptMatch(deliveryId),
        });
    },

    acquire(deliveryId: number, request?: DeliveryStatusUpdateRequest) {
        return apiRequest<null>({
            method: "PATCH",
            url: API_ENDPOINTS.shipper.acquire(deliveryId),
            data: request,
        });
    },

    requestConfirmation(
        deliveryId: number,
        request?: DeliveryStatusUpdateRequest,
    ) {
        return apiRequest<null>({
            method: "PATCH",
            url: API_ENDPOINTS.shipper.confirm(deliveryId),
            data: request,
        });
    },

    shipperLocation(request: DeliveryLocation) {
        return apiRequest<null>({
            method: "PUT",
            url: API_ENDPOINTS.shipper.location,
            data: request,
        });
    },
};
