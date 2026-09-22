import { useLocation, useNavigate } from "react-router-dom";
import PageHeader from "../components/common/PageHeader";
import { useState } from "react";
import {
    Report,
    REPORT_TYPE,
    ReportTargetType,
    ReportTypeKey,
} from "../types/report";
import { reportApi } from "../apis/reportApi";

type ReportLocationState = {
    targetType: ReportTargetType;
    deliveryId?: number;
    chatMessageId?: number;
    reportedAccountId?: number;
};

export default function ReportPage() {
    const navigate = useNavigate();
    const location = useLocation();
    const state = location.state as ReportLocationState | null;

    const [reason, setReason] = useState<ReportTypeKey | "">("");
    const [content, setContent] = useState("");
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [error, setError] = useState<string | null>(null);

    const [isReasonOpen, setIsReasonOpen] = useState(false);
    const selectedReasonLabel =
        reason !== "" ? REPORT_TYPE[reason].label : "신고 유형을 선택해주세요";

    const isValid =
        state !== null &&
        reason !== "" &&
        content.trim().length > 0 &&
        !isSubmitting;

    const handleSubmit = async () => {
        if (!state) {
            setError("신고 대상 정보를 확인할 수 없습니다.");
            return;
        }

        if (!reason || !content.trim() || isSubmitting) {
            return;
        }

        setIsSubmitting(true);
        setError(null);

        const request: Report = {
            targetType: state.targetType,
            reason,
            detail: content.trim(),
            imageKeys: [],
            deliveryId: state?.deliveryId ?? 0,
            chatMessageId: state?.chatMessageId ?? 0,
            reportedAccountId: state?.reportedAccountId ?? 0,
        };

        try {
            await reportApi.create(request);
            navigate(-1);
        } catch (error) {
            setError(
                error instanceof Error
                    ? error.message
                    : "신고를 접수하지 못했습니다.",
            );
        } finally {
            setIsSubmitting(false);
        }
    };

    return (
        <div className="flex flex-col page-container">
            <PageHeader title="신고하기" onBack={() => navigate(-1)} />
            <div className="mt-8 flex flex-col gap-3.5">
                <span className="text-sm text-gray-800 font-semibold">
                    신고유형
                </span>
                <div className="relative">
                    <button
                        type="button"
                        onClick={() => setIsReasonOpen((prev) => !prev)}
                        className="flex w-full items-center justify-between rounded-[10px] bg-gray-50 px-5 py-[15px] text-left"
                    >
                        <span
                            className={`text-[15px] font-medium ${
                                reason ? "text-gray-800" : "text-gray-400"
                            }`}
                        >
                            {selectedReasonLabel}
                        </span>

                        <svg
                            width="14"
                            height="8"
                            viewBox="0 0 14 8"
                            fill="none"
                        >
                            <path
                                d="M1 1L7 7L13 1"
                                stroke="#8E91A1"
                                strokeWidth="1.6"
                                strokeLinecap="round"
                                strokeLinejoin="round"
                            />
                        </svg>
                    </button>
                    {isReasonOpen ? (
                        <div className="absolute left-0 right-0 top-[58px] z-20 overflow-hidden rounded-[10px] border border-gray-100 bg-white shadow-lg">
                            {Object.entries(REPORT_TYPE).map(([key, value]) => {
                                const isSelected = reason === key;

                                return (
                                    <button
                                        key={key}
                                        type="button"
                                        onClick={() => {
                                            setReason(key as ReportTypeKey);
                                            setIsReasonOpen(false);
                                        }}
                                        className={`flex w-full items-center justify-between px-5 py-3.5 text-left text-[15px] transition-colors ${
                                            isSelected
                                                ? "bg-purple-50 font-semibold text-purple-600"
                                                : "text-gray-700 hover:bg-gray-50"
                                        }`}
                                    >
                                        <span>{value.label}</span>
                                    </button>
                                );
                            })}
                        </div>
                    ) : null}
                </div>
            </div>
            <div className="mt-7 flex flex-col gap-3.5">
                <span className="text-sm text-gray-800 font-semibold">
                    신고내용
                </span>
                <textarea
                    id="reportDetail"
                    value={content}
                    onChange={(event) => setContent(event.target.value)}
                    placeholder="신고 내용을 입력해주세요"
                    className="h-72 bg-gray-50 rounded-[10px] px-5 py-4 placeholder:text-gray-400 placeholder:font-semibold"
                />
                {error ? (
                    <p className="mt-3 text-sm text-errorRed">{error}</p>
                ) : null}
            </div>
            <div className="absolute bottom-5 left-4 right-4">
                <button
                    type="button"
                    onClick={() => void handleSubmit()}
                    disabled={!isValid}
                    className={`w-full rounded-lg py-3.5 text-base font-bold transition-colors ${
                        isValid
                            ? "bg-errorRed text-white"
                            : "cursor-not-allowed bg-gray-100 text-gray-900"
                    }`}
                >
                    {isSubmitting ? "신고 중..." : "신고하기"}
                </button>
            </div>
        </div>
    );
}
