import { type FormEvent, useState } from "react";
import { createPortal } from "react-dom";
import { useNavigate } from "react-router-dom";
import PageHeader from "../components/common/PageHeader";
import { inquiryApi } from "../apis";
import type { GeneralInquiryCategory } from "../apis/inquiryApi";
import { ApiError } from "../types/api";

const inquiryOptions: Array<{
    value: GeneralInquiryCategory;
    label: string;
}> = [
        { value: "ACCOUNT", label: "계정 및 회원 정보" },
        { value: "PAYMENT", label: "결제 및 포인트" },
        { value: "DELIVERY", label: "전달 서비스" },
        { value: "SERVICE", label: "서비스 이용" },
        { value: "BUG", label: "오류 및 버그 신고" },
        { value: "ETC", label: "기타" },
    ];

const MIN_CONTENT_LENGTH = 10;

function ChevronDownIcon() {
    return (
        <svg
            width="14"
            height="8"
            viewBox="0 0 14 8"
            fill="none"
            aria-hidden="true"
        >
            <path
                d="M1 1L7 7L13 1"
                stroke="currentColor"
                strokeWidth="1.6"
                strokeLinecap="round"
                strokeLinejoin="round"
            />
        </svg>
    );
}

export default function InquiryPage() {
    const navigate = useNavigate();
    const [selectedLabel, setSelectedLabel] = useState("");
    const [content, setContent] = useState("");
    const [errorMessage, setErrorMessage] = useState("");
    const [isConfirmOpen, setIsConfirmOpen] = useState(false);
    const [isSubmitting, setIsSubmitting] = useState(false);

    const selectedOption = inquiryOptions.find(
        (option) => option.label === selectedLabel,
    );

    const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
        event.preventDefault();

        if (!selectedOption) {
            setErrorMessage("문의 유형을 선택해주세요.");
            return;
        }

        if (content.trim().length < MIN_CONTENT_LENGTH) {
            setErrorMessage("문의 내용은 10자 이상 입력해주세요.");
            return;
        }

        setErrorMessage("");
        setIsConfirmOpen(true);
    };

    const handleFinalSubmit = async () => {
        if (!selectedOption || isSubmitting) {
            return;
        }

        setIsSubmitting(true);

        try {
            await inquiryApi.createGeneral({
                category: selectedOption.value,
                title: selectedOption.label,
                content: content.trim(),
            });
            setIsConfirmOpen(false);
            navigate("/home");
        } catch (caughtError) {
            setIsConfirmOpen(false);
            setErrorMessage(
                caughtError instanceof ApiError
                    ? caughtError.message
                    : "문의를 등록하지 못했습니다. 다시 시도해주세요.",
            );
        } finally {
            setIsSubmitting(false);
        }
    };

    return (
        <main className="page-container flex h-full min-h-0 flex-col overflow-hidden">
            <div
                className={`flex min-h-0 flex-1 flex-col ${isConfirmOpen ? "pointer-events-none" : ""
                    }`}
                aria-hidden={isConfirmOpen}
            >
                <PageHeader
                    title="문의하기"
                    onBack={() => navigate(-1)}
                    className="shrink-0"
                />

                <form
                    onSubmit={handleSubmit}
                    className="flex min-h-0 flex-1 flex-col"
                >
                    <div className="scrollbar-hidden min-h-0 flex-1 overflow-y-auto px-1 pb-6 pt-5">
                        <div className="flex flex-col gap-7">
                            <label className="flex flex-col gap-3.5">
                                <span className="text-sm font-semibold leading-[22px] text-gray-800">
                                    문의유형
                                </span>
                                <div className="relative">
                                    <select
                                        value={selectedLabel}
                                        onChange={(event) => {
                                            setSelectedLabel(
                                                event.target.value,
                                            );
                                            setErrorMessage("");
                                        }}
                                        className="h-[52px] w-full appearance-none rounded-[10px] bg-gray-50 px-5 pr-12 text-[15px] font-semibold leading-[22px] text-gray-800 outline-none"
                                    >
                                        <option value="">
                                            문의 유형을 선택해주세요
                                        </option>
                                        {inquiryOptions.map((option) => (
                                            <option
                                                key={option.label}
                                                value={option.label}
                                            >
                                                {option.label}
                                            </option>
                                        ))}
                                    </select>
                                    <span
                                        className="pointer-events-none absolute right-5 top-1/2 flex h-2 w-3.5 -translate-y-1/2 items-center justify-center text-gray-800"
                                        aria-hidden="true"
                                    >
                                        <ChevronDownIcon />
                                    </span>
                                </div>
                            </label>

                            <label className="flex flex-col gap-3.5">
                                <span className="text-sm font-semibold leading-[22px] text-gray-800">
                                    문의내용
                                </span>
                                <textarea
                                    value={content}
                                    onChange={(event) => {
                                        setContent(event.target.value);
                                        setErrorMessage("");
                                    }}
                                    placeholder="문의 내용을 입력해주세요."
                                    className="h-[312px] w-full resize-none rounded-[10px] bg-gray-50 px-5 py-[15px] text-[15px] font-semibold leading-[22px] text-gray-800 outline-none placeholder:text-gray-400"
                                />
                            </label>

                            {errorMessage ? (
                                <p
                                    className="text-xs font-semibold leading-5 text-red-500"
                                    role="alert"
                                >
                                    {errorMessage}
                                </p>
                            ) : null}
                        </div>
                    </div>

                    <button
                        type="submit"
                        className="w-full shrink-0 rounded-[10px] bg-purple-500 px-2.5 py-3.5 text-base font-bold leading-[22px] text-white transition-colors hover:bg-purple-600 focus:outline-none"
                    >
                        등록하기
                    </button>
                </form>
            </div>

            {isConfirmOpen
                ? createPortal(
                    <div
                        className="fixed inset-0 z-[100] flex items-center justify-center bg-black/40 px-8 backdrop-blur-sm"
                        role="dialog"
                        aria-modal="true"
                        onClick={
                            isSubmitting
                                ? undefined
                                : () => setIsConfirmOpen(false)
                        }
                    >
                        <div
                            className="w-full max-w-[338px] rounded-[14px] bg-white px-5 py-6 text-center shadow-lg"
                            onClick={(event) => event.stopPropagation()}
                        >
                            <p className="text-[18px] font-semibold leading-[25px] text-gray-900">
                                제출하시겠습니까?
                            </p>
                            <p className="mt-2 text-[14px] font-medium leading-[22px] text-[#70727E]">
                                작성하신 문의를 제출합니다.
                            </p>
                            <div className="mt-5 flex gap-3">
                                <button
                                    type="button"
                                    onClick={() => setIsConfirmOpen(false)}
                                    disabled={isSubmitting}
                                    className="flex flex-1 items-center justify-center rounded-lg bg-gray-100 py-3.5 font-bold text-gray-700 transition hover:bg-gray-200 focus:outline-none disabled:opacity-60"
                                >
                                    아니오
                                </button>
                                <button
                                    type="button"
                                    onClick={handleFinalSubmit}
                                    disabled={isSubmitting}
                                    className="flex flex-1 items-center justify-center rounded-lg bg-purple-500 py-3.5 font-bold text-white transition hover:bg-purple-600 focus:outline-none disabled:opacity-60"
                                >
                                    {isSubmitting ? "등록 중..." : "예"}
                                </button>
                            </div>
                        </div>
                    </div>,
                    document.body,
                )
                : null}
        </main>
    );
}
