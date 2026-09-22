import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { accountApi } from "../apis/accountApi";
import StudentVerificationModal from "../components/verification/StudentVerificationModal";
import { ApiError } from "../types/api";
import type { UserRole } from "../types/user";
import { setCurrentUserRole } from "../utils/auth";

export default function UserStateChoice() {
    const navigate = useNavigate();
    const [selectedType, setSelectedType] = useState<UserRole | null>(null);
    const [isVerificationOpen, setIsVerificationOpen] = useState(false);
    const [isCheckingStudent, setIsCheckingStudent] = useState(false);
    const [studentCheckError, setStudentCheckError] = useState("");

    const handleHome = async () => {
        if (!selectedType || isCheckingStudent) {
            return;
        }

        if (selectedType === "sender") {
            setCurrentUserRole("sender");
            navigate("/home");
            return;
        }

        setIsCheckingStudent(true);
        setStudentCheckError("");

        try {
            const isStudentVerified = await accountApi.checkStudent();

            if (!isStudentVerified) {
                setIsVerificationOpen(true);
                return;
            }

            setCurrentUserRole("shipper");
            navigate("/home");
        } catch (error) {
            setStudentCheckError(
                error instanceof ApiError
                    ? error.message
                    : "학생 인증 여부를 확인하지 못했습니다.",
            );
        } finally {
            setIsCheckingStudent(false);
        }
    };

    const handleVerificationComplete = () => {
        setCurrentUserRole("shipper");
        navigate("/home");
    };

    return (
        <>
            <div className="page-container relative flex flex-col items-center justify-center">
                <img
                    className="h-[clamp(200px,29dvh,250px)] w-[clamp(200px,29dvh,250px)] shrink-0 object-contain"
                    src="/Logo.png"
                    alt="Logo"
                    width={250}
                    height={250}
                />

                <div className="mt-6 flex w-full flex-col gap-3">
                    <button
                        type="button"
                        disabled={isCheckingStudent}
                        onClick={() => setSelectedType("sender")}
                        className={`shadow-[0px_0px_3px_0px_rgba(0,_0,_0,_0.1)] w-full rounded-lg p-5 transition-colors ${
                            selectedType === "sender"
                                ? "bg-gray-500 text-white"
                                : "bg-gray-50 text-gray-600 hover:bg-gray-100"
                        }`}
                    >
                        물건을 보내고 싶어요!
                    </button>
                    <button
                        type="button"
                        disabled={isCheckingStudent}
                        onClick={() => setSelectedType("shipper")}
                        className={`shadow-[0px_0px_3px_0px_rgba(0,_0,_0,_0.1)] w-full rounded-lg p-5 transition-colors ${
                            selectedType === "shipper"
                                ? "bg-gray-500 text-white"
                                : "bg-gray-50 text-gray-600 hover:bg-gray-100"
                        }`}
                    >
                        물건을 전달하고 싶어요!
                    </button>
                </div>

                <div className="absolute bottom-5 left-5 right-5">
                    {studentCheckError ? (
                        <p
                            className="mb-2 text-center text-xs text-red-500"
                            role="alert"
                        >
                            {studentCheckError}
                        </p>
                    ) : null}
                    <button
                        type="button"
                        disabled={selectedType === null || isCheckingStudent}
                        onClick={() => void handleHome()}
                        className={`w-full rounded-lg p-3.5 font-semibold shadow-[0px_0px_3px_0px_rgba(0,_0,_0,_0.1)] transition-colors ${
                            selectedType && !isCheckingStudent
                                ? "cursor-pointer bg-purple-500 text-white hover:bg-purple-600"
                                : "cursor-not-allowed bg-gray-100 text-gray-400"
                        }`}
                    >
                        {isCheckingStudent
                            ? "학생 인증 확인 중..."
                            : "패스로 시작하기"}
                    </button>
                </div>
            </div>

            {isVerificationOpen ? (
                <StudentVerificationModal
                    onComplete={handleVerificationComplete}
                    onClose={() => setIsVerificationOpen(false)}
                />
            ) : null}
        </>
    );
}
