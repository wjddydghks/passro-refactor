import { type FormEvent, useState } from "react";
import { useNavigate } from "react-router-dom";
import PageHeader from "../components/common/PageHeader";
import FeedbackModal from "../components/signup/common/FeedbackModal";
import ValidationMessage from "../components/signup/common/ValidationMessage";
import { getPasswordValidation } from "../utils/signupValidation";
import { authApi } from "../apis/authApi";
import { ApiError } from "../types/api";

const inputClassName =
    "w-full rounded-[10px] bg-gray-50 px-5 py-[15px] text-[15px] font-medium leading-[22px] text-gray-800 outline-none placeholder:text-gray-500";

export default function ChangePasswordPage() {
    const navigate = useNavigate();
    const [currentPassword, setCurrentPassword] = useState("");
    const [newPassword, setNewPassword] = useState("");
    const [newPasswordCheck, setNewPasswordCheck] = useState("");
    const [showValidation, setShowValidation] = useState(false);
    const [submissionMessage, setSubmissionMessage] = useState("");
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [isSuccess, setIsSuccess] = useState(false);
    const isNewPasswordValid = getPasswordValidation(newPassword);
    const isPasswordCheckValid =
        newPasswordCheck.length > 0 && newPassword === newPasswordCheck;

    const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
        event.preventDefault();
        setShowValidation(true);
        setSubmissionMessage("");
        setIsSuccess(false);

        if (
            !currentPassword.trim() ||
            !isNewPasswordValid ||
            !isPasswordCheckValid ||
            isSubmitting
        ) {
            return;
        }

        setIsSubmitting(true);

        try {
            await authApi.editPassword({
                nowPassword: currentPassword,
                editPassword: newPassword,
            });
            setIsSuccess(true);
            setCurrentPassword("");
            setNewPassword("");
            setNewPasswordCheck("");
            setShowValidation(false);
        } catch (error) {
            setSubmissionMessage(
                error instanceof ApiError
                    ? error.message
                    : "비밀번호 변경에 실패했습니다. 다시 시도해주세요.",
            );
        } finally {
            setIsSubmitting(false);
        }
    };

    return (
        <main className="page-container flex flex-col overflow-hidden">
            <PageHeader
                title="비밀번호 변경"
                onBack={() => navigate(-1)}
                className="shrink-0"
            />

            <form
                onSubmit={handleSubmit}
                className="flex min-h-0 flex-1 flex-col"
            >
                <div className="scrollbar-hidden min-h-0 flex-1 overflow-y-auto px-1 pb-6 pt-8">
                    <div className="flex flex-col gap-6">
                        <label className="flex flex-col gap-1.5">
                            <span className="text-sm font-semibold leading-[22px] text-gray-700">
                                현재 비밀번호
                            </span>
                            <input
                                type="password"
                                value={currentPassword}
                                onChange={(event) => {
                                    setCurrentPassword(event.target.value);
                                    setSubmissionMessage("");
                                }}
                                placeholder="현재 비밀번호를 확인해주세요"
                                className={inputClassName}
                                autoComplete="current-password"
                            />
                            <ValidationMessage
                                message="현재 비밀번호를 입력해주세요"
                                fallback=""
                                visible={
                                    showValidation && !currentPassword.trim()
                                }
                            />
                        </label>

                        <label className="flex flex-col gap-1.5">
                            <span className="text-sm font-semibold leading-[22px] text-gray-700">
                                신규 비밀번호
                            </span>
                            <input
                                type="password"
                                value={newPassword}
                                onChange={(event) => {
                                    setNewPassword(event.target.value);
                                    setSubmissionMessage("");
                                }}
                                placeholder="신규 비밀번호를 입력해주세요"
                                className={inputClassName}
                                autoComplete="new-password"
                            />
                            <p
                                className={`pl-1 text-[11px] font-medium leading-[18px] min-[390px]:whitespace-nowrap min-[390px]:text-[12px] ${isNewPasswordValid
                                        ? "text-[#24A148]"
                                        : "text-[#E5484D]"
                                    }`}
                                aria-live="polite"
                            >
                                6~20자 영문 대문자, 소문자, 숫자, 특수문자 중
                                2가지 이상 조합
                            </p>
                        </label>

                        <label className="flex flex-col gap-1.5">
                            <span className="text-sm font-semibold leading-[22px] text-gray-700">
                                신규 비밀번호 확인
                            </span>
                            <input
                                type="password"
                                value={newPasswordCheck}
                                onChange={(event) => {
                                    setNewPasswordCheck(event.target.value);
                                    setSubmissionMessage("");
                                }}
                                placeholder="신규 비밀번호를 확인해주세요"
                                className={inputClassName}
                                autoComplete="new-password"
                            />
                            <ValidationMessage
                                message={
                                    isPasswordCheckValid
                                        ? "비밀번호가 일치합니다"
                                        : "비밀번호가 일치하는지 확인해주세요"
                                }
                                fallback=""
                                colorClass={
                                    isPasswordCheckValid
                                        ? "text-[#24A148]"
                                        : "text-[#E5484D]"
                                }
                            />
                        </label>

                        {submissionMessage ? (
                            <p
                                className="text-xs font-semibold text-red-500"
                                role="alert"
                            >
                                {submissionMessage}
                            </p>
                        ) : null}
                    </div>
                </div>

                <button
                    type="submit"
                    disabled={isSubmitting}
                    className="w-full shrink-0 rounded-[10px] bg-purple-500 px-2.5 py-3.5 text-base font-bold leading-[22px] text-white transition-colors hover:bg-purple-600 focus:outline-none disabled:cursor-not-allowed disabled:opacity-60"
                >
                    {isSubmitting ? "변경 중..." : "비밀번호 변경하기"}
                </button>
            </form>

            <FeedbackModal
                title="비밀번호 변경 완료"
                message={isSuccess ? "비밀번호가 변경되었습니다." : ""}
                onClose={() => navigate("/mypage/edit")}
            />
        </main>
    );
}
