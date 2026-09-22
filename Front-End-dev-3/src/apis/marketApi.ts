import { apiRequest } from "./client";
import { API_ENDPOINTS } from "./endpoints";

export type MarketCategory = "음식" | "카페" | "편의점" | "기타";

export interface MarketItem {
    id: number;
    name: string;
    price: number;
    category: MarketCategory;
    imageKey: string | null;
}

export interface MarketPurchase {
    item: MarketItem;
    beforePoint: number;
    usedPoint: number;
    remainingPoint: number;
}

export const marketApi = {
    getItems(category?: MarketCategory) {
        return apiRequest<MarketItem[] | null>({
            method: "GET",
            url: API_ENDPOINTS.market.root,
            params: category ? { category } : undefined,
        });
    },

    purchase(marketId: number) {
        return apiRequest<MarketPurchase>({
            method: "POST",
            url: API_ENDPOINTS.market.purchase(marketId),
        });
    },
};
