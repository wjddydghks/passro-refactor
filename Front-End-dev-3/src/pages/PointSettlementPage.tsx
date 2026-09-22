import { useNavigate } from "react-router-dom";
import PageHeader from "../components/common/PageHeader";

export default function PointSettlementPage() {
    const navigate = useNavigate();

    return (
        <div className="page-container">
            <div className="relative flex min-h-full w-full flex-col bg-white">
                
                {/* 상단바 영역 */}
                <div className="sticky top-0 z-50 w-full bg-white">
                    <PageHeader title="포인트 정산" onBack={() => navigate(-1)} />
                </div>

                {/* 상단 완료 섹션 */}
                <div className="flex flex-col items-center mt-10 w-full px-6">
                    {/* 체크 아이콘 동그라미 */}
                    <div className="w-16 h-16 bg-purple-200 rounded-full flex items-center justify-center text-purple-600 mb-5">
                        <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" strokeWidth={3} stroke="currentColor" className="w-7 h-7">
                            <path strokeLinecap="round" strokeLinejoin="round" d="M4.5 12.75l6 6 9-13.5" />
                        </svg>
                    </div>

                    <h2 className="text-xl font-bold text-gray-900 mb-2">
                        정산이 완료되었어요!
                    </h2>
                    <p className="text-sm text-gray-500 font-medium text-center leading-relaxed">
                        마이페이지에서<br />자세한 내역을 확인할 수 있어요
                    </p>
                </div>

                {/* 중단 상세 요금 내역 섹션 */}
                <div className="mt-10 px-6 space-y-4">
                    <h3 className="text-[15px] font-bold text-gray-500 mb-5">
                        정산 금액
                    </h3>

                    {/* 기본요금 */}
                    <div className="flex items-center justify-between text-[15px]">
                        <span className="text-gray-400 font-medium">기본요금</span>
                        <span className="text-gray-600 font-bold">2,500 P</span>
                    </div>

                    {/* 거리요금 */}
                    <div className="flex items-center justify-between text-[15px]">
                        <span className="text-gray-400 font-medium">거리요금</span>
                        <span className="text-gray-600 font-bold">+ 200 P</span>
                    </div>

                    {/* 무게요금 */}
                    <div className="flex items-center justify-between text-[15px]">
                        <span className="text-gray-400 font-medium">무게요금</span>
                        <span className="text-gray-600 font-bold">+ 500 P</span>
                    </div>
                </div>

                {/* 하단 최종 정산 금액 카드 */}
                <div className="mt-8 px-6 py-3">
                    <div className="flex items-center justify-between bg-gray-50 rounded-xl p-5">
                        <span className="text-lg font-bold text-gray-800">최종 정산 금액</span>
                        <span className="text-2xl font-extrabold text-purple-600">3,200 P</span>
                    </div>
                </div>

                {/* 확인 버튼 */}
                <div className="mt-8 w-full px-6 pb-6">
                    <button className="w-full bg-purple-500 text-white font-bold py-4 rounded-xl shadow-sm active:bg-indigo-700 transition focus:outline-none">
                        확인
                    </button>
                </div>

            </div>
        </div>
    )
}
