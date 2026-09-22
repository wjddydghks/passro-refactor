import { apiRequest } from "./client";
import { API_ENDPOINTS } from "./endpoints";

export interface SubwayStationItem {
    id: number;
    region?: string;
    stationName: string;
    routeName: string;
}

export interface SubwayPathRequest {
    originPlaceId: number;
    destinationPlaceId: number;
    waypointPlaceIds: Array<number>;
}

export interface SubwayPathItem {
    shortestDistance: number;
    transferCount: number;
    stations: Array<SubwayStationItem>;
}

export const subwayApi = {
    search(keyword: string) {
        return apiRequest<SubwayStationItem[]>({
            method: "GET",
            url: API_ENDPOINTS.subway.search,
            params: { keyword },
        });
    },

    path(request: SubwayPathRequest) {
        return apiRequest<SubwayPathItem>({
            method: "POST",
            url: API_ENDPOINTS.subway.path,
            data: request,
        });
    },
};
