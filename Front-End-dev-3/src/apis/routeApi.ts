export interface CommuteRoute {
    id: number;
    name: string;
    origin: string;
    destination: string;
    days: string[];
    startTime: string;
    endTime: string;
    favorite: boolean;
}

export type SaveCommuteRouteRequest = Omit<CommuteRoute, "id">;

/**
 * 통학 경로 CRUD 명세가 확정되면 이 계약을 구현합니다.
 */
export interface RouteApiContract {
    getRoutes(): Promise<CommuteRoute[]>;
    createRoute(request: SaveCommuteRouteRequest): Promise<CommuteRoute>;
    updateRoute(
        routeId: number,
        request: SaveCommuteRouteRequest,
    ): Promise<CommuteRoute>;
    deleteRoute(routeId: number): Promise<void>;
}
