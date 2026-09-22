import { Point } from "../types/point";
import { apiRequest } from "./client";
import { API_ENDPOINTS } from "./endpoints";

export const pointApi = {
    getHistory() {
        return apiRequest<Point>({
            method: "GET",
            url: API_ENDPOINTS.account.points,
        });
    },
};
