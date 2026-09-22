import { useCallback, useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { fileApi } from "../apis/fileApi";
import {
    marketApi,
    type MarketCategory,
    type MarketItem,
} from "../apis/marketApi";
import { pointApi } from "../apis/pointApi";
import dividerImage from "../assets/images/market/divider.svg";
import PageHeader from "../components/common/PageHeader";
import FeedbackModal from "../components/signup/common/FeedbackModal";
import { useApiRequest } from "../hooks/useApiRequest";
import { ApiError } from "../types/api";
import { imgproxied } from "../utils/img";

type MarketFilter = "전체" | MarketCategory;

type MarketProduct = MarketItem & {
    imageUrl: string | null;
};

const CATEGORIES: MarketFilter[] = ["전체", "음식", "카페", "편의점", "기타"];

const pointFormatter = new Intl.NumberFormat("ko-KR");

function getFallbackImage() {
    return null;
}

async function resolveProductImage(item: MarketItem) {
    if (!item.imageKey) {
        return getFallbackImage();
    }

    try {
        return await fileApi.getImageDownloadUrl(item.imageKey);
    } catch {
        return getFallbackImage();
    }
}

function ProductItem({
    product,
    isPurchasing,
    onPurchase,
}: {
    product: MarketProduct;
    isPurchasing: boolean;
    onPurchase: (product: MarketProduct) => void;
}) {
    return (
        <article className="flex flex-1 min-h-[70px] items-center gap-2.5 overflow-hidden">
            {product.imageUrl ? (
                <img
                    src={imgproxied(product.imageUrl, 100)}
                    alt={product.name}
                    className="h-[60px] w-[60px] shrink-0 object-cover rounded-[10px]"
                />
            ) : (
                <div
                    className="h-[60px] w-[60px] shrink-0 rounded bg-gray-50"
                    role="img"
                    aria-label={`${product.name} 이미지 없음`}
                />
            )}

            <div className="flex min-w-0 flex-1 flex-col justify-between py-0.5">
                <h2 className="truncate text-base font-bold text-[14px] text-gray-800">
                    {product.name}
                </h2>
                <p className="text-[12px] font-semibold leading-[14px] text-gray-500">
                    {pointFormatter.format(product.price)}P
                </p>
            </div>

            <button
                type="button"
                onClick={() => onPurchase(product)}
                disabled={isPurchasing}
                className="shrink-0 rounded-[10px] bg-purple-100 px-2.5 py-2 text-sm font-bold leading-4 text-purple-700 disabled:cursor-wait disabled:opacity-60"
                aria-label={`${product.name} 교환하기`}
            >
                {isPurchasing ? "처리 중" : "교환하기"}
            </button>
        </article>
    );
}

export default function MarketPage() {
    const navigate = useNavigate();
    const [selectedCategory, setSelectedCategory] =
        useState<MarketFilter>("전체");
    const [selectedProduct, setSelectedProduct] =
        useState<MarketProduct | null>(null);
    const [isPurchasing, setIsPurchasing] = useState(false);
    const [feedbackTitle, setFeedbackTitle] = useState("");
    const [feedbackMessage, setFeedbackMessage] = useState("");
    const [purchasedPoint, setPurchasedPoint] = useState<number | null>(null);
    const loadPointHistory = useCallback(() => pointApi.getHistory(), []);
    const loadMarketProducts = useCallback(async (filter: MarketFilter) => {
        const items =
            (await marketApi.getItems(
                filter === "전체" ? undefined : filter,
            )) ?? [];

        return Promise.all(
            items.map(async (item) => ({
                ...item,
                imageUrl: await resolveProductImage(item),
            })),
        );
    }, []);
    const pointRequest = useApiRequest(loadPointHistory);
    const marketRequest = useApiRequest(loadMarketProducts);

    useEffect(() => {
        void pointRequest.execute().catch(() => undefined);
    }, [pointRequest.execute]);

    useEffect(() => {
        void marketRequest.execute(selectedCategory).catch(() => undefined);
    }, [marketRequest.execute, selectedCategory]);

    const handlePurchase = async () => {
        if (!selectedProduct || isPurchasing) {
            return;
        }

        setIsPurchasing(true);

        try {
            const result = await marketApi.purchase(selectedProduct.id);
            setPurchasedPoint(result.remainingPoint);
            setSelectedProduct(null);
            setFeedbackTitle("교환 완료");
            setFeedbackMessage(`${result.item.name} 교환이 완료되었습니다.`);
        } catch (error) {
            setSelectedProduct(null);
            setFeedbackTitle("교환 실패");
            setFeedbackMessage(
                error instanceof ApiError
                    ? error.message
                    : "상품을 교환하지 못했습니다. 다시 시도해주세요.",
            );
        } finally {
            setIsPurchasing(false);
        }
    };

    const currentPoint = purchasedPoint ?? pointRequest.data?.currentPoint ?? 0;
    const products = marketRequest.data ?? [];

    return (
        <main className="page-container flex h-full min-h-0 flex-col overflow-hidden bg-white px-[21px]">
            <PageHeader title="마켓" className="shrink-0" />
            <div className="flex flex-col flex-1 min-h-0 pb-6 pt-3.5">
                <section
                    className="flex h-[80px] items-center rounded-[10px] bg-purple-100 px-5 py-[15px]"
                    aria-labelledby="market-point-title"
                >
                    <div className="flex w-full items-center justify-between">
                        <div className="leading-[22px] text-gray-800">
                            <p
                                id="market-point-title"
                                className="text-[14px] font-medium text-gray-600"
                            >
                                보유 포인트
                            </p>
                            <p className="text-[22px] font-bold text-purple-700">
                                {pointRequest.isLoading
                                    ? "불러오는 중"
                                    : `${pointFormatter.format(currentPoint)}P`}
                            </p>
                        </div>
                        <button
                            type="button"
                            onClick={() => navigate("/mypage/point")}
                            className="rounded-lg bg-purple-600 px-2.5 py-1 text-xs font-bold leading-[22px] text-white"
                        >
                            포인트 내역
                        </button>
                    </div>
                </section>

                {pointRequest.error ? (
                    <button
                        type="button"
                        onClick={() =>
                            void pointRequest.execute().catch(() => undefined)
                        }
                        className="mt-2 w-full text-right text-xs font-medium text-red-500 underline"
                    >
                        포인트 다시 불러오기
                    </button>
                ) : null}

                <nav
                    className="mt-[32px] flex items-center gap-x-[6px]"
                    aria-label="상품 카테고리"
                >
                    {CATEGORIES.map((category) => {
                        const isSelected = selectedCategory === category;

                        return (
                            <button
                                key={category}
                                type="button"
                                onClick={() => setSelectedCategory(category)}
                                className={`rounded-[10px] px-[12px] py-[4px] text-xs font-bold leading-[22px] transition-colors ${
                                    isSelected
                                        ? "bg-gray-900 text-white box-border"
                                        : "bg-white text-gray-800 border-gray-200 border-[1px] box-border"
                                }`}
                                aria-pressed={isSelected}
                            >
                                {category}
                            </button>
                        );
                    })}
                </nav>

                <div className="mt-[22px] font-bold text-[16px] text-gray-800">
                    추천상품
                </div>

                <section
                    className="mt-[14px] flex flex-col gap-[8px] scrollbar-hidden min-h-0 flex-1 overflow-y-auto"
                    aria-label="마켓 상품"
                >
                    {marketRequest.isLoading ? (
                        <div className="flex flex-col gap-2">
                            {Array.from({ length: 4 }).map((_, index) => (
                                <div
                                    key={index}
                                    className="h-[98px] w-full animate-pulse rounded-[10px] bg-gray-50"
                                    aria-hidden="true"
                                />
                            ))}
                        </div>
                    ) : marketRequest.error ? (
                        <div className="py-10 text-center" role="alert">
                            <p className="text-sm font-medium text-errorRed">
                                마켓 상품을 불러오지 못했습니다.
                            </p>
                            <button
                                type="button"
                                onClick={() =>
                                    void marketRequest
                                        .execute(selectedCategory)
                                        .catch(() => undefined)
                                }
                                className="mt-2 text-xs font-bold text-errorRed underline"
                            >
                                다시 시도
                            </button>
                        </div>
                    ) : products.length > 0 ? (
                        products.map((product, index) => (
                            <div
                                className="w-full flex p-[14px] bg-gray-50 rounded-[10px]"
                                key={product.id}
                            >
                                <ProductItem
                                    product={product}
                                    isPurchasing={
                                        isPurchasing &&
                                        selectedProduct?.id === product.id
                                    }
                                    onPurchase={setSelectedProduct}
                                />
                            </div>
                        ))
                    ) : (
                        <p className="py-10 text-center text-sm font-medium text-gray-500">
                            해당 카테고리의 상품이 없습니다.
                        </p>
                    )}
                </section>
            </div>

            <FeedbackModal
                title={selectedProduct ? "상품 교환" : feedbackTitle}
                message={
                    selectedProduct
                        ? `${selectedProduct.name} 상품을 ${pointFormatter.format(selectedProduct.price)}P로 교환하시겠습니까?`
                        : feedbackMessage
                }
                onClose={() => {
                    if (isPurchasing) {
                        return;
                    }
                    setSelectedProduct(null);
                    setFeedbackTitle("");
                    setFeedbackMessage("");
                }}
                actionLabel={selectedProduct ? "교환하기" : undefined}
                onAction={
                    selectedProduct ? () => void handlePurchase() : undefined
                }
                isActionDisabled={isPurchasing}
            />
        </main>
    );
}
