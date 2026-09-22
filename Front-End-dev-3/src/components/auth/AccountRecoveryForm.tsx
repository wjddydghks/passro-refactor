import { type FormEvent, useState } from "react";
import { useNavigate } from "react-router-dom";
import { authApi } from "../../apis/authApi";
import { ApiError } from "../../types/api";
import { formatPhoneNumber } from "../../utils/signupFormatters";
import PageHeader from "../common/PageHeader";
import FeedbackModal from "../signup/common/FeedbackModal";

type RecoveryType = "id" | "password";

interface AccountRecoveryFormProps {
    type: RecoveryType;
}

type RecoveryValues = {
    name: string;
    phone: string;
    email: string;
};

const recoveryConfig = {
    id: {
        title: "아이디 찾기",
        description: "회원가입 하셨던 아이디로 이메일로 보내드릴게요!",
        buttonLabel: "이메일 전송하기",
    },
    password: {
        title: "비밀번호 찾기",
        description: (
            <>
                회원가입 하셨던 아이디로 이메일로
                <br />
                임시 비밀번호를 보내드릴게요!
            </>
        ),
        buttonLabel: "임시 비밀번호 전송하기",
    },
} as const;

const inputClassName =
    "w-full rounded-[10px] bg-gray-50 px-5 py-[15px] text-[15px] font-medium leading-[22px] text-gray-900 outline-none placeholder:text-gray-500";

export default function AccountRecoveryForm({
    type,
}: AccountRecoveryFormProps) {
    const navigate = useNavigate();
    const config = recoveryConfig[type];
    const [values, setValues] = useState<RecoveryValues>({
        name: "",
        phone: "",
        email: "",
    });
    const [errorMessage, setErrorMessage] = useState("");
    const [modalMessage, setModalMessage] = useState("");
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [isComplete, setIsComplete] = useState(false);

    const updateValue = (key: keyof RecoveryValues, value: string) => {
        setValues((previous) => ({ ...previous, [key]: value }));
        setErrorMessage("");
    };

    const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
        event.preventDefault();

        if (!values.name.trim() || !values.phone.trim()) {
            setErrorMessage("필수 정보를 모두 입력해 주세요.");
            return;
        }

        if (!/^01[016789]-?\d{3,4}-?\d{4}$/.test(values.phone)) {
            setErrorMessage("올바른 전화번호 형식으로 입력해 주세요.");
            return;
        }

        if (
            type === "password" &&
            !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(values.email.trim())
        ) {
            setErrorMessage("올바른 이메일 형식으로 입력해 주세요.");
            return;
        }

        setIsSubmitting(true);
        setErrorMessage("");
        setModalMessage("");

        try {
            if (type === "id") {
                await authApi.findId({
                    name: values.name.trim(),
                    phoneNumber: values.phone,
                });
                setModalMessage(
                    "입력한 정보와 일치하는 계정이 있다면 가입 이메일로 아이디를 발송했습니다.",
                );
            } else {
                await authApi.findPassword({
                    name: values.name.trim(),
                    phoneNumber: values.phone,
                    mail: values.email.trim(),
                });
                setModalMessage(
                    "입력한 정보와 일치하는 계정이 있다면 이메일로 임시 비밀번호를 발송했습니다.",
                );
            }
            setIsComplete(true);
        } catch (error) {
            setErrorMessage(
                error instanceof ApiError
                    ? error.message
                    : "계정 정보 확인 중 오류가 발생했습니다.",
            );
        } finally {
            setIsSubmitting(false);
        }
    };

    return (
        <main className="page-container flex h-dvh min-h-0 flex-col overflow-hidden">
            <PageHeader
                title={config.title}
                onBack={() => navigate(-1)}
                className="shrink-0"
            />

            <form
                className="flex min-h-0 flex-1 flex-col px-1"
                onSubmit={handleSubmit}
                noValidate
            >
                <div className="scrollbar-hidden flex-1 space-y-6 overflow-y-auto pb-6 pt-12">
                    <label className="block">
                        <span className="mb-1 block text-sm font-semibold leading-[22px] text-gray-700">
                            이름
                        </span>
                        <input
                            type="text"
                            value={values.name}
                            onChange={(event) =>
                                updateValue("name", event.target.value)
                            }
                            placeholder="회원가입 시 작성했던 이름을 입력해주세요"
                            className={inputClassName}
                            autoComplete="name"
                        />
                    </label>

                    <label className="block">
                        <span className="mb-1 block text-sm font-semibold leading-[22px] text-gray-700">
                            전화번호
                        </span>
                        <input
                            type="tel"
                            value={values.phone}
                            onChange={(event) =>
                                updateValue(
                                    "phone",
                                    formatPhoneNumber(event.target.value),
                                )
                            }
                            placeholder="회원가입 시 작성했던 전화번호를 입력해주세요"
                            className={inputClassName}
                            autoComplete="tel"
                            inputMode="tel"
                        />
                    </label>

                    {type === "password" ? (
                        <label className="block">
                            <span className="mb-1 block text-sm font-semibold leading-[22px] text-gray-700">
                                이메일
                            </span>
                            <input
                                type="email"
                                value={values.email}
                                onChange={(event) =>
                                    updateValue("email", event.target.value)
                                }
                                placeholder="이메일을 입력해주세요"
                                className={inputClassName}
                                autoComplete="email"
                                inputMode="email"
                            />
                        </label>
                    ) : null}

                    {errorMessage ? (
                        <p className="text-xs font-semibold text-red-500" role="alert">
                            {errorMessage}
                        </p>
                    ) : null}
                </div>

                <div className="shrink-0 pt-4 text-center">
                    <p className="mb-4 text-[13px] font-medium leading-[22px] text-purple-600">
                        {config.description}
                    </p>
                    <button
                        type="submit"
                        disabled={isSubmitting}
                        className="w-full rounded-[10px] bg-purple-500 px-2.5 py-3.5 text-base font-bold leading-[22px] text-white transition-colors hover:bg-purple-600 focus:outline-none disabled:cursor-not-allowed disabled:opacity-60"
                    >
                        {isSubmitting ? "전송 중..." : config.buttonLabel}
                    </button>
                </div>
            </form>

            <FeedbackModal
                message={modalMessage}
                onClose={() => {
                    setModalMessage("");
                    if (isComplete) {
                        navigate("/login", { replace: true });
                    }
                }}
            />
        </main>
    );
}
