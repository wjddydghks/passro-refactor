import { apiRequest } from "./client";
import { API_ENDPOINTS } from "./endpoints";

export type NotificationType = "GENERAL" | "DELIVERY";
export type NotificationResourceType = "NONE" | "DELIVERY";

export interface NotificationItem {
    notificationId: number;
    type: NotificationType;
    title: string;
    content: string;
    resourceType: NotificationResourceType;
    resourceId: number | null;
    readAt: string | null;
    read: boolean;
    createdAt: string;
}

export interface NotificationPageParams {
    page?: number;
    size?: number;
}

export interface NotificationSort {
    empty: boolean;
    sorted: boolean;
    unsorted: boolean;
}

export interface NotificationPageable {
    offset: number;
    sort: NotificationSort;
    paged: boolean;
    pageNumber: number;
    pageSize: number;
    unpaged: boolean;
}

export interface NotificationPage {
    totalElements: number;
    totalPages: number;
    size: number;
    content: NotificationItem[];
    number: number;
    sort: NotificationSort;
    first: boolean;
    last: boolean;
    numberOfElements: number;
    pageable: NotificationPageable;
    empty: boolean;
}

export interface UnreadNotificationCount {
    unreadCount: number;
}

export const notificationApi = {
    getNotifications(params?: NotificationPageParams) {
        return apiRequest<NotificationPage>({
            method: "GET",
            url: API_ENDPOINTS.notification.root,
            params,
        });
    },

    getUnreadCount() {
        return apiRequest<UnreadNotificationCount>({
            method: "GET",
            url: API_ENDPOINTS.notification.unreadCount,
        });
    },

    markAsRead(notificationId: number) {
        return apiRequest<NotificationItem>({
            method: "PATCH",
            url: API_ENDPOINTS.notification.read(notificationId),
        });
    },

    deleteNotification(notificationId: number) {
        return apiRequest<null>({
            method: "DELETE",
            url: API_ENDPOINTS.notification.detail(notificationId),
        });
    },
};
