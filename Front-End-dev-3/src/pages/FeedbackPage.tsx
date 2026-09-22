import { useNavigate, useParams } from "react-router-dom";
import PageHeader from "../components/common/PageHeader";
import { useCallback, useEffect, useState } from "react";
import { reviewApi, senderDeliveryApi } from "../apis";
import { getCurrentUser } from "../utils/auth";
import { useApiRequest } from "../hooks/useApiRequest";

const REVIEW_TAGS = [
    "응답이 빨라요",
    "친절해요",
    "매너가 좋아요",
    "시간 약속 철저해요",
] as const;

export default function FeedbackPage() {
    const navigate = useNavigate();

    const { deliveryId: deliveryIdParam } = useParams<{ deliveryId: string }>();
    const deliveryId = Number(deliveryIdParam);

    const [rating, setRating] = useState(0);
    const [selectedTags, setSelectedTags] = useState<string[]>([]);
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [error, setError] = useState<string | null>(null);

    const loadDeliveryDetail = useCallback(() => {
        if (!Number.isSafeInteger(deliveryId) || deliveryId <= 0) {
            return Promise.reject(new Error("올바르지 않은 전달 ID입니다."));
        }

        return senderDeliveryApi.getDeliveryItem(deliveryId);
    }, [deliveryId]);

    const detailRequest = useApiRequest(loadDeliveryDetail);

    useEffect(() => {
        void detailRequest.execute().catch(() => undefined);
    }, [detailRequest.execute]);

    const currentUser = getCurrentUser();

    const reviewerName =
        currentUser?.nickname ?? currentUser?.name ?? "패스로 사용자";

    const shipperName = detailRequest.data?.shipperInfo?.name ?? "전달자";

    const handleTagClick = (tag: string) => {
        setSelectedTags((prev) =>
            prev.includes(tag)
                ? prev.filter((item) => item !== tag)
                : [...prev, tag],
        );
    };

    const handleSubmit = async () => {
        if (!Number.isSafeInteger(deliveryId) || deliveryId <= 0) {
            setError("올바르지 않은 전달 ID입니다.");
            return;
        }

        if (rating === 0) {
            setError("별점을 선택해주세요.");
            return;
        }

        setIsSubmitting(true);
        setError(null);

        try {
            await reviewApi.create({
                deliveryId,
                rating,
                content:
                    selectedTags.length > 0
                        ? selectedTags.join(", ")
                        : undefined,
            });

            navigate("/home", { replace: true });
        } catch (error) {
            setError(
                error instanceof Error
                    ? error.message
                    : "리뷰를 등록하지 못했습니다.",
            );
        } finally {
            setIsSubmitting(false);
        }
    };

    return (
        <div className="page-container flex flex-col overflow-hidden">
            {/* 상단바 영역 */}
            <div className="w-full shrink-0 bg-white">
                <PageHeader title="완료확인" onBack={() => navigate(-1)} />
            </div>

            {/* 중앙 피드백 콘텐츠 영역 */}
            <div className="scrollbar-hidden flex flex-1 flex-col items-center justify-center space-y-12 overflow-y-auto px-6 py-6">
                {/* 1. 상단 질문 & 별점 섹션 */}
                <div className="flex flex-col items-center w-full text-center">
                    <h2 className="text-[19px] font-bold text-gray-900 leading-snug mb-6">
                        {reviewerName}님, <br />
                        {shipperName}님의 전달은 어떠셨나요?
                    </h2>

                    {/* 5성점 별 아이콘 세트 */}
                    <div className="flex items-center space-x-2">
                        {[...Array(5)].map((_, i) => (
                            <button
                                key={i}
                                type="button"
                                onClick={() => {
                                    setRating(i + 1);
                                    setError(null);
                                }}
                            >
                                <svg
                                    key={i}
                                    xmlns="http://www.w3.org/2000/svg"
                                    fill="currentColor"
                                    viewBox="0 0 24 24"
                                    className={`w-10 h-10 cursor-pointer transition ${i <= rating - 1 ? "text-yellow-400" : "text-gray-100"}`}
                                >
                                    <path d="M11.48 3.499a.562.562 0 011.04 0l2.125 5.111a.563.563 0 00.475.345l5.518.442c.499.04.701.663.321.988l-4.204 3.602a.563.563 0 00-.182.557l1.285 5.385a.562.562 0 01-.84.61l-4.725-2.885a.563.563 0 00-.586 0L6.982 20.54a.562.562 0 01-.84-.61l1.285-5.386a.562.562 0 00-.182-.557l-4.204-3.602a.563.563 0 01.321-.988l5.518-.442a.563.563 0 00.475-.345L11.48 3.5z" />
                                </svg>
                            </button>
                        ))}
                    </div>
                </div>

                {/* 2. 하단 태그 선택 섹션 */}
                <div className="flex flex-col items-center w-full text-center">
                    <h3 className="text-[18px] font-bold text-gray-900 mb-6">
                        어떤 점이 좋았나요?
                    </h3>

                    {/* 태그 배치 정렬 */}
                    <div className="flex flex-wrap justify-center gap-3">
                        {REVIEW_TAGS.map((tag) => {
                            const isSelected = selectedTags.includes(tag);

                            return (
                                <button
                                    key={tag}
                                    type="button"
                                    onClick={() => handleTagClick(tag)}
                                    className={`px-5 py-2.5 rounded-full text-[14px] font-medium focus:outline-none whitespace-nowrap ${
                                        isSelected
                                            ? "bg-purple-600 text-gray-100"
                                            : "bg-gray-50 text-gray-700 hover:bg-gray-200"
                                    }`}
                                >
                                    {tag}
                                </button>
                            );
                        })}
                    </div>
                </div>
                {error ? (
                    <div className="text-red-500 text-[14px] mt-4">{error}</div>
                ) : null}
            </div>

            {/* 3. 하단 고정 완료하기 버튼 영역 */}
            <div className="w-full shrink-0 px-6">
                <button
                    onClick={handleSubmit}
                    disabled={isSubmitting}
                    className="w-full bg-purple-500 text-white text-[16px] font-bold py-4 rounded-xl shadow-sm active:bg-indigo-700 transition focus:outline-none"
                >
                    {isSubmitting ? "등록 중..." : "완료하기"}
                </button>
            </div>
        </div>
    );
}
