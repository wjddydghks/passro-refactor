import {
    CreateDeliveryRequest,
    DeliveryPayment,
    DeliveryPaymentRequest,
    SenderDeliveryDetail,
    SenderDeliveryListItem,
} from "../../types/delivery/sender";
import { apiRequest } from "../client";
import { API_ENDPOINTS } from "../endpoints";

export const senderDeliveryApi = {
    getDeliveryList() {
        return apiRequest<SenderDeliveryListItem[]>({
            method: "GET",
            url: API_ENDPOINTS.sender.root,
        });
    },

    getDeliveryItem(deliveryId: number) {
        return apiRequest<SenderDeliveryDetail>({
            method: "GET",
            url: API_ENDPOINTS.sender.detail(deliveryId),
        });
    },

    getPayment(request: DeliveryPaymentRequest) {
        return apiRequest<DeliveryPayment>({
            method: "GET",
            url: API_ENDPOINTS.sender.payment,
            params: request,
        });
    },

    create(request: CreateDeliveryRequest) {
        return apiRequest<string>({
            method: "POST",
            url: API_ENDPOINTS.sender.root,
            data: request,
        });
    },

    agreeTerms(deliveryId: number) {
        return apiRequest<null>({
            method: "PATCH",
            url: API_ENDPOINTS.sender.terms(deliveryId),
        });
    },

    complete(deliveryId: number) {
        return apiRequest<null>({
            method: "PATCH",
            url: API_ENDPOINTS.sender.complete(deliveryId),
        });
    },

    cancel(deliveryId: number) {
        return apiRequest<null>({
            method: "PATCH",
            url: API_ENDPOINTS.sender.cancel(deliveryId),
        });
    },

    location(deliveryId: number) {
        return apiRequest<null>({
            method: "GET",
            url: API_ENDPOINTS.sender.location(deliveryId),
        });
    },
};
