import type { FormEvent } from "react";
import { useState } from "react";
import { useNavigate } from "react-router-dom";
import PageHeader from "../components/common/PageHeader";
import { BasicSignupForm, DetailSignupForm } from "../components/signup";
import type {
    SignupEmailVerificationStatus,
    SignupFieldUpdater,
    SignupFormData,
    SignupRouteStations,
    SignupStep,
} from "../types/signup";
import { authApi } from "../apis/authApi";
import { ApiError } from "../types/api";
import FeedbackModal from "../components/signup/common/FeedbackModal";

const initialSignupFormData: SignupFormData = {
    nickname: "",
    email: "",
    emailCode: "",
    password: "",
    passwordCheck: "",
    name: "",
    phone: "",
    birthDate: "",
    gender: "NONE",
    address: "",
    originStationId: -1,
    destinationStationId: -1,
    wayPoints: [],
};

const initialRouteStations: SignupRouteStations = {
    origin: null,
    destination: null,
    wayPoints: [],
};

export default function SignupPage() {
    const navigate = useNavigate();
    const [step, setStep] = useState<SignupStep>("basic");
    const [formData, setFormData] = useState<SignupFormData>(
        initialSignupFormData,
    );
    const [routeStations, setRouteStations] =
        useState<SignupRouteStations>(initialRouteStations);
    const [emailVerificationStatus, setEmailVerificationStatus] =
        useState<SignupEmailVerificationStatus>("idle");
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [modalMessage, setModalMessage] = useState("");
    const [isSignupComplete, setIsSignupComplete] = useState(false);

    const updateField: SignupFieldUpdater = (key, value) => {
        setFormData((prev) => ({
            ...prev,
            [key]: value,
        }));
    };

    const handleBasicSubmit = (e: FormEvent<HTMLFormElement>) => {
        e.preventDefault();
        setStep("detail");
    };

    const handleFinalSubmit = async (e: FormEvent<HTMLFormElement>) => {
        e.preventDefault();

        if (
            formData.originStationId <= 0 ||
            formData.destinationStationId <= 0
        ) {
            setModalMessage("출발역과 도착역을 선택해주세요.");
            return;
        }

        if (formData.originStationId === formData.destinationStationId) {
            setModalMessage("출발역과 도착역은 서로 달라야 합니다.");
            return;
        }

        const routeStationIds = [
            formData.originStationId,
            ...formData.wayPoints,
            formData.destinationStationId,
        ];

        if (new Set(routeStationIds).size !== routeStationIds.length) {
            setModalMessage("출발역, 경유역, 도착역은 서로 달라야 합니다.");
            return;
        }

        setIsSubmitting(true);
        setModalMessage("");

        try {
            await authApi.signup({
                mail: formData.email.trim(),
                password: formData.password,
                nickname: formData.nickname.trim(),
                name: formData.name.trim(),
                phoneNumber: formData.phone,
                birth: formData.birthDate,
                sourceStationId: formData.originStationId,
                destinationStationId: formData.destinationStationId,
                wayPoints: formData.wayPoints,
            });
            setIsSignupComplete(true);
            setModalMessage("회원가입이 완료되었습니다.");
        } catch (error) {
            setModalMessage(
                error instanceof ApiError
                    ? error.message
                    : "회원가입 중 오류가 발생했습니다.",
            );
        } finally {
            setIsSubmitting(false);
        }
    };

    const handleBack = () => {
        if (step === "basic") {
            navigate(-1);
            return;
        }

        setStep("basic");
    };

    return (
        <div className="page-container relative flex flex-col overflow-hidden pb-5">
            <PageHeader
                title={step === "basic" ? "회원가입" : "상세정보"}
                onBack={handleBack}
                className="shrink-0"
            />

            {step === "basic" ? (
                <BasicSignupForm
                    formData={formData}
                    updateField={updateField}
                    onSubmit={handleBasicSubmit}
                    emailVerificationStatus={emailVerificationStatus}
                    onEmailVerificationStatusChange={setEmailVerificationStatus}
                />
            ) : (
                <DetailSignupForm
                    formData={formData}
                    updateField={updateField}
                    routeStations={routeStations}
                    onRouteStationsChange={setRouteStations}
                    onSubmit={handleFinalSubmit}
                    isSubmitting={isSubmitting}
                />
            )}

            <FeedbackModal
                message={modalMessage}
                onClose={() => {
                    setModalMessage("");
                    if (isSignupComplete) {
                        navigate("/login", { replace: true });
                    }
                }}
            />
        </div>
    );
}
