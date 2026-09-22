import { apiRequest } from "./client";
import { API_ENDPOINTS } from "./endpoints";

export type GeneralInquiryCategory =
    | "ACCOUNT"
    | "PAYMENT"
    | "DELIVERY"
    | "SERVICE"
    | "BUG"
    | "ETC";

export type DeliveryInquiryCategory =
    | "DELAY"
    | "DAMAGE"
    | "LOST"
    | "WRONG_DELIVERY"
    | "POINT"
    | "ETC";

export type InquiryCategory = DeliveryInquiryCategory;

export interface CreateGeneralInquiryRequest {
    category: GeneralInquiryCategory;
    title: string;
    content: string;
}

export interface GeneralInquiry {
    inquiryId: number;
    category: GeneralInquiryCategory;
    title: string;
    content: string;
    writerNickname: string | null;
    createdAt: string;
}

export interface CreateDeliveryInquiryRequest {
    deliveryId: number;
    category: DeliveryInquiryCategory;
    title?: string;
    content: string;
}

export interface DeliveryInquiry {
    inquiryId: number;
    deliveryId: number;
    category: DeliveryInquiryCategory;
    title: string | null;
    content: string;
    writerNickname: string | null;
    createdAt: string;
}

export const inquiryApi = {
    createGeneral(request: CreateGeneralInquiryRequest) {
        return apiRequest<GeneralInquiry>({
            method: "POST",
            url: API_ENDPOINTS.inquiry.root,
            data: request,
        });
    },

    createDelivery(request: CreateDeliveryInquiryRequest) {
        return apiRequest<DeliveryInquiry>({
            method: "POST",
            url: API_ENDPOINTS.deliveryInquiry.root,
            data: request,
        });
    },

    getByDelivery(deliveryId: number) {
        return apiRequest<DeliveryInquiry[]>({
            method: "GET",
            url: API_ENDPOINTS.deliveryInquiry.byDelivery(deliveryId),
        });
    },
};
