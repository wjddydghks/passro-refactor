import { Report } from "../types/report";
import { apiRequest } from "./client";
import { API_ENDPOINTS } from "./endpoints";

export const reportApi = {
    create(request: Report) {
        return apiRequest<null>({
            method: "POST",
            url: API_ENDPOINTS.report.root,
            data: request,
        });
    },
};
