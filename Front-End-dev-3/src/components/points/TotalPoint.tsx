interface TotalPointProps {
    total?: number;
    isLoading?: boolean;
}
export const TotalPoint = ({ total, isLoading = false }: TotalPointProps) => {
    return (
        <div className="flex flex-col mt-9 w-full rounded-xl px-6 py-5 gap-5 bg-gradient-to-r from-[#636DFF] to-[#4541EB]">
            <div className="flex flex-col gap-1.5">
                <div className="font-semibold text-sm text-purple-200">
                    보유 포인트
                </div>
                {isLoading ? (
                    <div
                        className="h-7 w-28 animate-pulse rounded bg-white/30 h-[32px]"
                        aria-label="보유 포인트를 불러오는 중"
                    />
                ) : (
                    <div className="font-bold text-2xl text-white h-[32px]">
                        {(total ?? 0).toLocaleString()}P
                    </div>
                )}
            </div>
            <div className="flex items-center justify-center w-full px-2.5 py-2 rounded-lg bg-purple-100 text-purple-700 font-semibold">
                + 충전하기
            </div>
        </div>
    );
};
