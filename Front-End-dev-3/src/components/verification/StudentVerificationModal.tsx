import { useState } from "react";
import { authApi } from "../../apis/authApi";
import { ApiError } from "../../types/api";

type VerificationStep = "intro" | "form" | "complete";

interface StudentVerificationModalProps {
    onComplete: () => void;
    onClose: () => void;
}

const modalClassName =
    "relative z-10 w-[calc(100%-40px)] max-w-[360px] rounded-[10px] bg-white";

export default function StudentVerificationModal({
    onComplete,
    onClose,
}: StudentVerificationModalProps) {
    const [step, setStep] = useState<VerificationStep>("intro");
    const [email, setEmail] = useState("");
    const [verificationCode, setVerificationCode] = useState("");
    const [isCodeRequested, setIsCodeRequested] = useState(false);
    const [emailAction, setEmailAction] = useState<
        "send" | "confirm" | null
    >(null);
    const [feedbackMessage, setFeedbackMessage] = useState("");
    const [hasError, setHasError] = useState(false);
    const canRequestCode = Boolean(email.trim()) && emailAction === null;
    const canConfirmCode =
        isCodeRequested &&
        verificationCode.length === 6 &&
        emailAction === null;

    const handleRequestCode = async () => {
        const mail = email.trim();
        if (!mail || emailAction) {
            return;
        }

        setEmailAction("send");
        setFeedbackMessage("");
        setHasError(false);

        try {
            await authApi.sendMail({ mail, student: true });
            setIsCodeRequested(true);
            setVerificationCode("");
            setFeedbackMessage("학교 이메일로 인증번호를 전송했습니다.");
        } catch (error) {
            setHasError(true);
            setFeedbackMessage(
                error instanceof ApiError
                    ? error.message
                    : "인증번호를 전송하지 못했습니다.",
            );
        } finally {
            setEmailAction(null);
        }
    };

    const handleVerify = async () => {
        const mail = email.trim();
        const code = verificationCode.trim();
        if (!isCodeRequested || !mail || !code || emailAction) {
            return;
        }

        setEmailAction("confirm");
        setFeedbackMessage("");
        setHasError(false);

        try {
            await authApi.confirmUniversityMail({ mail, code });
            setStep("complete");
        } catch (error) {
            setHasError(true);
            setFeedbackMessage(
                error instanceof ApiError
                    ? error.message
                    : "인증번호를 확인하지 못했습니다.",
            );
        } finally {
            setEmailAction(null);
        }
    };

    return (
        <div
            className="fixed inset-y-0 left-1/2 z-50 flex w-full max-w-[402px] -translate-x-1/2 items-center justify-center bg-black/40"
            role="dialog"
            aria-modal="true"
            aria-labelledby={`student-verification-${step}-title`}
            onClick={onClose}
        >
            {step === "intro" ? (
                <section
                    className={`${modalClassName} flex flex-col items-center gap-[25px] px-[10px] pb-[22px] pt-[27px] text-center`}
                    onClick={(event) => event.stopPropagation()}
                >
                    <div className="flex flex-col items-center gap-[10px]">
                        <h2
                            id="student-verification-intro-title"
                            className="text-[18px] font-semibold leading-[25px] text-gray-900"
                        >
                            물건을 전달하려면
                            <br />
                            학생 인증이 필요해요!
                        </h2>
                        <p className="text-[13px] font-medium text-gray-400">
                            학교 이메일을 통해 인증할 수 있어요.
                        </p>
                    </div>

                    <button
                        type="button"
                        onClick={() => setStep("form")}
                        className="flex h-[51px] w-full max-w-[320px] items-center justify-center rounded-[10px] bg-purple-500 px-[10px] py-[14px] text-[16px] font-bold leading-[22px] text-white"
                    >
                        인증하기
                    </button>
                </section>
            ) : null}

            {step === "form" ? (
                <section
                    className={`${modalClassName} flex flex-col gap-[10px] px-[22px] py-[25px]`}
                    onClick={(event) => event.stopPropagation()}
                >
                    <h2 id="student-verification-form-title" className="sr-only">
                        학생 이메일 인증
                    </h2>

                    <div className="flex items-end gap-2">
                        <label className="flex min-w-0 flex-1 flex-col gap-[10px]">
                            <span className="text-[14px] font-semibold leading-[22px] text-gray-700">
                                아이디(이메일)
                            </span>
                            <input
                                type="email"
                                value={email}
                                onChange={(event) => {
                                    setEmail(event.target.value);
                                    setVerificationCode("");
                                    setIsCodeRequested(false);
                                    setFeedbackMessage("");
                                    setHasError(false);
                                }}
                                placeholder="이메일을 입력해주세요"
                                className="h-[52px] min-w-0 rounded-[10px] bg-gray-50 px-5 text-[15px] font-medium text-gray-800 outline-none placeholder:text-gray-500"
                                autoComplete="email"
                            />
                        </label>
                        <button
                            type="button"
                            onClick={handleRequestCode}
                            disabled={!canRequestCode}
                            className={`flex h-[52px] w-[90px] shrink-0 items-center justify-center rounded-[10px] text-[15px] font-medium leading-[22px] transition-colors ${
                                canRequestCode
                                    ? "bg-purple-600 text-white hover:bg-[#918DFF]"
                                    : "cursor-not-allowed bg-gray-200 text-gray-400"
                            }`}
                        >
                            {emailAction === "send" ? "전송 중..." : "인증 요청"}
                        </button>
                    </div>

                    <div className="flex items-end gap-2">
                        <label className="flex min-w-0 flex-1 flex-col gap-[10px]">
                            <span className="text-[14px] font-semibold leading-[22px] text-gray-700">
                                인증번호
                            </span>
                            <input
                                type="text"
                                value={verificationCode}
                                onChange={(event) => {
                                    setVerificationCode(
                                        event.target.value
                                            .replace(/\D/g, "")
                                            .slice(0, 6),
                                    );
                                    setFeedbackMessage("");
                                    setHasError(false);
                                }}
                                placeholder="인증번호를 입력해주세요"
                                className="h-[52px] min-w-0 rounded-[10px] bg-gray-50 px-5 text-[15px] font-medium text-gray-800 outline-none placeholder:text-gray-500"
                                inputMode="numeric"
                                maxLength={6}
                                autoComplete="one-time-code"
                                disabled={!isCodeRequested || emailAction !== null}
                            />
                        </label>
                        <button
                            type="button"
                            onClick={handleVerify}
                            disabled={!canConfirmCode}
                            className={`flex h-[52px] w-[90px] shrink-0 items-center justify-center rounded-[10px] text-[15px] font-medium leading-[22px] transition-colors ${
                                canConfirmCode
                                    ? "bg-purple-600 text-white hover:bg-[#918DFF]"
                                    : "cursor-not-allowed bg-gray-200 text-gray-400"
                            }`}
                        >
                            {emailAction === "confirm" ? "확인 중..." : "확인"}
                        </button>
                    </div>

                    {feedbackMessage ? (
                        <p
                            className={`px-1 text-[12px] ${
                                hasError ? "text-red-500" : "text-purple-500"
                            }`}
                            role={hasError ? "alert" : "status"}
                        >
                            {feedbackMessage}
                        </p>
                    ) : null}
                </section>
            ) : null}

            {step === "complete" ? (
                <section
                    className={`${modalClassName} flex flex-col items-center gap-[25px] px-[10px] pb-[22px] pt-[27px] text-center`}
                    onClick={(event) => event.stopPropagation()}
                >
                    <div className="flex flex-col items-center gap-[10px]">
                        <h2
                            id="student-verification-complete-title"
                            className="text-[18px] font-semibold leading-[25px] text-gray-900"
                        >
                            인증되었어요!
                        </h2>
                        <p className="text-[13px] font-medium text-gray-400">
                            이제 패스로 활동할 수 있어요.
                        </p>
                    </div>

                    <button
                        type="button"
                        onClick={onComplete}
                        className="flex h-[51px] w-full max-w-[320px] items-center justify-center rounded-[10px] bg-purple-500 px-[10px] py-[14px] text-[16px] font-bold leading-[22px] text-white"
                    >
                        확인
                    </button>
                </section>
            ) : null}
        </div>
    );
}
