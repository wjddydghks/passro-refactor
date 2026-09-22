import { type FormEvent, useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import FeedbackModal from "../components/signup/common/FeedbackModal";
import { login } from "../utils/auth";
import { ApiError } from "../types/api";
import { isEmailValid } from "../utils/signupValidation";

type LoginLocationState = {
    from?: {
        pathname?: string;
    };
};

type LoginFieldErrors = {
    email?: string;
    password?: string;
};

export default function LoginPage() {
    const navigate = useNavigate();
    const location = useLocation();
    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const [fieldErrors, setFieldErrors] = useState<LoginFieldErrors>({});
    const [errorMessage, setErrorMessage] = useState("");
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [isServerErrorOpen, setIsServerErrorOpen] = useState(false);

    const state = location.state as LoginLocationState | null;
    const from = state?.from?.pathname ?? "/user-state-choice";

    const validateFields = () => {
        const nextErrors: LoginFieldErrors = {};
        const trimmedEmail = email.trim();

        if (!trimmedEmail) {
            nextErrors.email = "이메일을 입력해 주세요.";
        } else if (!isEmailValid(trimmedEmail)) {
            nextErrors.email = "올바른 이메일 형식으로 입력해 주세요.";
        }

        if (!password.trim()) {
            nextErrors.password = "비밀번호를 입력해 주세요.";
        }

        setFieldErrors(nextErrors);
        return Object.keys(nextErrors).length === 0;
    };

    const executeLogin = async () => {
        setErrorMessage("");
        setIsServerErrorOpen(false);
        setIsSubmitting(true);

        try {
            await login(email, password);
            navigate(from, { replace: true });
        } catch (error) {
            const isTemporaryFailure =
                !(error instanceof ApiError) ||
                error.status === undefined ||
                error.status >= 500 ||
                error.status === 429;

            if (isTemporaryFailure) {
                setIsServerErrorOpen(true);
            } else {
                setErrorMessage(
                    "이메일 또는 비밀번호가 일치하지 않습니다.",
                );
            }
        } finally {
            setIsSubmitting(false);
        }
    };

    const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
        event.preventDefault();

        if (!validateFields()) {
            return;
        }

        void executeLogin();
    };

    const handleEmailChange = (value: string) => {
        setEmail(value);
        setErrorMessage("");
        setFieldErrors((current) => ({ ...current, email: undefined }));
    };

    const handlePasswordChange = (value: string) => {
        setPassword(value);
        setErrorMessage("");
        setFieldErrors((current) => ({ ...current, password: undefined }));
    };

    return (
        <div className="page-container flex flex-col items-center justify-center gap-6">
            <img
                className="h-[clamp(200px,29dvh,250px)] w-[clamp(200px,29dvh,250px)] shrink-0 object-contain"
                src="/Logo.png"
                alt="Logo"
                width={250}
                height={250}
            />

            <form
                className="w-full flex flex-col gap-2.5"
                onSubmit={handleSubmit}
                noValidate
            >
                <div>
                    <input
                        type="email"
                        placeholder="아이디를 입력해 주세요"
                        value={email}
                        onChange={(event) =>
                            handleEmailChange(event.target.value)
                        }
                        className={`w-full rounded-lg border bg-gray-50 px-5 py-4 text-sm outline-none shadow-[0px_0px_3px_0px_rgba(0,_0,_0,_0.1)] placeholder:font-semibold placeholder:text-gray-400 ${
                            fieldErrors.email
                                ? "border-rose-500"
                                : "border-transparent"
                        }`}
                        aria-label="아이디"
                        aria-invalid={Boolean(fieldErrors.email)}
                        aria-describedby={
                            fieldErrors.email ? "login-email-error" : undefined
                        }
                        autoComplete="email"
                    />
                    {fieldErrors.email ? (
                        <p
                            id="login-email-error"
                            className="mt-1 px-1 text-xs font-semibold text-rose-600"
                            role="alert"
                        >
                            {fieldErrors.email}
                        </p>
                    ) : null}
                </div>
                <div className="mb-5">
                    <input
                        type="password"
                        placeholder="비밀번호를 입력해 주세요"
                        value={password}
                        onChange={(event) =>
                            handlePasswordChange(event.target.value)
                        }
                        className={`w-full rounded-lg border bg-gray-50 px-5 py-4 text-sm outline-none shadow-[0px_0px_3px_0px_rgba(0,_0,_0,_0.1)] placeholder:font-semibold placeholder:text-gray-400 ${
                            fieldErrors.password
                                ? "border-rose-500"
                                : "border-transparent"
                        }`}
                        aria-label="비밀번호"
                        aria-invalid={Boolean(fieldErrors.password)}
                        aria-describedby={
                            fieldErrors.password
                                ? "login-password-error"
                                : undefined
                        }
                        autoComplete="current-password"
                    />
                    {fieldErrors.password ? (
                        <p
                            id="login-password-error"
                            className="mt-1 px-1 text-xs font-semibold text-rose-600"
                            role="alert"
                        >
                            {fieldErrors.password}
                        </p>
                    ) : null}
                </div>

                {errorMessage ? (
                    <p
                        className="-mt-3 text-xs font-semibold text-red-500"
                        role="alert"
                    >
                        {errorMessage}
                    </p>
                ) : null}

                <button
                    type="submit"
                    disabled={isSubmitting}
                    className="shadow-[0px_0px_3px_0px_rgba(0,_0,_0,_0.1)] w-full bg-purple-500 text-white rounded-lg py-4 font-bold disabled:cursor-not-allowed disabled:opacity-60"
                >
                    {isSubmitting ? "로그인 중..." : "로그인"}
                </button>
            </form>

            <div className="flex items-center justify-center gap-3 mt-4 text-sm text-gray-600">
                <Link
                    to="/find-id"
                    className="hover:text-gray-900 hover:underline"
                >
                    아이디 찾기
                </Link>

                <span className="h-3 w-px bg-gray-200" />

                <Link
                    to="/find-password"
                    className="hover:text-gray-900 hover:underline"
                >
                    비밀번호 찾기
                </Link>

                <span className="h-3 w-px bg-gray-200" />

                <Link
                    to="/signup"
                    className="font-semibold hover:text-gray-900 hover:underline"
                >
                    회원가입
                </Link>
            </div>

            <FeedbackModal
                title="일시적인 오류가 발생했습니다."
                message={
                    isServerErrorOpen ? "잠시 후 다시 시도해 주세요." : ""
                }
                actionLabel={isSubmitting ? "다시 시도 중..." : "다시 시도"}
                isActionDisabled={isSubmitting}
                onAction={() => void executeLogin()}
                onClose={() => setIsServerErrorOpen(false)}
            />
        </div>
    );
}
