import type { BackendDeliveryState } from "../types/backend";
import { apiRequest } from "./client";
import { API_ENDPOINTS } from "./endpoints";

export interface ChatMessage {
    id: number;
    senderId: number;
    senderNickname: string;
    content: string;
    imageKey: string | null;
    systemMessage: boolean;
    isRead: boolean;
    createdAt: string;
}

export interface SendMessageRequest {
    content: string;
}

export interface ChatRoomInfo {
    partnerNickname: string;
    partnerPicture: string | null;
    itemName: string | null;
    departure: string | null;
    arrival: string | null;
    deliveryStatus: BackendDeliveryState;
}

export interface ChatRoomListElement {
    deliveryId: number;
    partner: {
        id: number;
        nickname: string;
        picture: string | null;
    };
    itemName: string | null;
    lastMessage: string | null;
    lastMessageAt: string | null;
    unreadCount: number;
}

export const chatApi = {
    getMessages(deliveryId: number, afterId?: number) {
        return apiRequest<ChatMessage[]>({
            method: "GET",
            url: API_ENDPOINTS.chat.messages(deliveryId),
            params: afterId === undefined ? undefined : { afterId },
        });
    },

    sendMessage(deliveryId: number, request: SendMessageRequest) {
        return apiRequest<ChatMessage>({
            method: "POST",
            url: API_ENDPOINTS.chat.messages(deliveryId),
            data: request,
        });
    },

    getRoomInfo(deliveryId: number) {
        return apiRequest<ChatRoomInfo>({
            method: "GET",
            url: API_ENDPOINTS.chat.info(deliveryId),
        });
    },

    getUnreadCount(deliveryId: number) {
        return apiRequest<number>({
            method: "GET",
            url: API_ENDPOINTS.chat.unreadCount(deliveryId),
        });
    },

    getRooms() {
        return apiRequest<ChatRoomListElement[]>({
            method: "GET",
            url: API_ENDPOINTS.chat.rooms,
        });
    },

    exitRoom(deliveryId: number){
        return apiRequest<null>({
            method: "DELETE",
            url: API_ENDPOINTS.chat.exit(deliveryId)
        });
    }
};
