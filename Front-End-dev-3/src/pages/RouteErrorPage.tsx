import {
    isRouteErrorResponse,
    useNavigate,
    useRouteError,
} from "react-router-dom";
import { isAuthenticated } from "../utils/auth";

export default function RouteErrorPage() {
    const navigate = useNavigate();
    const error = useRouteError();
    const message = isRouteErrorResponse(error)
        ? error.statusText || "요청한 화면을 불러오지 못했습니다."
        : error instanceof Error
          ? error.message
          : "예상하지 못한 오류가 발생했습니다.";

    return (
        <main className="flex min-h-dvh w-full flex-col items-center justify-center bg-white px-6 text-center">
            <div className="flex h-20 w-20 items-center justify-center rounded-full bg-red-50 text-3xl font-bold text-red-500">
                !
            </div>
            <h1 className="mt-6 text-xl font-bold text-gray-900">
                화면을 불러오지 못했어요
            </h1>
            <p className="mt-3 max-w-80 break-words text-sm font-medium leading-6 text-gray-500">
                {message}
            </p>

            <div className="mt-10 flex w-full max-w-[360px] gap-3">
                <button
                    type="button"
                    onClick={() => window.location.reload()}
                    className="flex-1 rounded-[10px] bg-gray-100 px-3 py-3.5 font-bold text-gray-700 hover:bg-gray-200 focus:outline-none"
                >
                    다시 시도
                </button>
                <button
                    type="button"
                    onClick={() =>
                        navigate(isAuthenticated() ? "/home" : "/login", {
                            replace: true,
                        })
                    }
                    className="flex-1 rounded-[10px] bg-purple-500 px-3 py-3.5 font-bold text-white hover:bg-purple-600 focus:outline-none"
                >
                    {isAuthenticated() ? "홈으로" : "로그인으로"}
                </button>
            </div>
        </main>
    );
}
