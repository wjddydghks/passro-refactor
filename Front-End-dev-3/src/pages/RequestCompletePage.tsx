import { useEffect } from "react";
import { useNavigate } from "react-router-dom";

export default function RequestCompletePage() {
    const navigate = useNavigate();
    useEffect(() => {
        const timer = setTimeout(() => {
            navigate("/home", { replace: true });
        }, 3000);

        return () => clearTimeout(timer);
    }, [navigate]);

    return (
        <div className="page-container flex h-[calc(100vh - 64px)] min-h-0 flex-col overflow-hidden">
            <div className="scrollbar-hidden flex flex-1 flex-col items-center justify-center overflow-y-auto px-6 pb-6 pt-12">
                {/* 로고 이미지 */}
                <img
                    src="/Logo.png"
                    alt="Logo"
                    width={250}
                    height={250}
                    className="object-contain mb-6 max-w-[60%]" // 화면 크기에 맞춰 로고도 유연하게 조절
                />

                {/* 완료 문구 */}
                <div className="text-xl font-bold text-gray-900 text-center">
                    등록이 완료되었어요!
                </div>
            </div>

            {/* 완료하기 */}
            <div className="w-full shrink-0 px-6">
                <button
                    type="button"
                    onClick={() => navigate("/home", { replace: true })}
                    className="w-full bg-purple-500 text-white font-bold py-3.5 rounded-xl shadow-sm active:bg-indigo-700 transition focus:outline-none"
                >
                    완료하기
                </button>
            </div>
        </div>
    );
}
