import { CreateReview, ReviewRequest } from "../types/delivery/sender";
import { apiRequest } from "./client";
import { API_ENDPOINTS } from "./endpoints";

export const reviewApi = {
    create(request: CreateReview) {
        return apiRequest<string>({
            method: "POST",
            url: API_ENDPOINTS.review.root,
            data: request,
        });
    },

    getAverage(userId: number) {
        return apiRequest<ReviewRequest>({
            method: "GET",
            url: API_ENDPOINTS.review.average(userId),
        });
    },
};
