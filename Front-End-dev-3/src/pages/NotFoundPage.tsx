import { useNavigate } from "react-router-dom";
import { isAuthenticated } from "../utils/auth";

export default function NotFoundPage() {
    const navigate = useNavigate();
    const destination = isAuthenticated() ? "/home" : "/login";

    return (
        <main className="flex min-h-dvh w-full flex-col items-center justify-center bg-white px-6 text-center">
            <p className="text-7xl font-extrabold text-purple-200">404</p>
            <h1 className="mt-5 text-xl font-bold text-gray-900">
                페이지를 찾을 수 없어요
            </h1>
            <p className="mt-3 text-sm font-medium leading-6 text-gray-500">
                주소가 잘못되었거나 이동한 페이지입니다.
                <br />
                이전 화면으로 돌아가 다시 시도해주세요.
            </p>

            <div className="mt-10 flex w-full max-w-[360px] gap-3">
                <button
                    type="button"
                    onClick={() => navigate(-1)}
                    className="flex-1 rounded-[10px] bg-gray-100 px-3 py-3.5 font-bold text-gray-700 hover:bg-gray-200 focus:outline-none"
                >
                    이전으로
                </button>
                <button
                    type="button"
                    onClick={() => navigate(destination, { replace: true })}
                    className="flex-1 rounded-[10px] bg-purple-500 px-3 py-3.5 font-bold text-white hover:bg-purple-600 focus:outline-none"
                >
                    {isAuthenticated() ? "홈으로" : "로그인으로"}
                </button>
            </div>
        </main>
    );
}
