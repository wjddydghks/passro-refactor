interface SizeInfoProps {
    arrowLeft?: string;
    boxTranslate?: string;
}
export const SizeInfo = ({
    boxTranslate = "-24%",
    arrowLeft = "21%",
}: SizeInfoProps) => {
    return (
        <div
            className="pointer-events-none absolute left-1/2 top-9 z-50 w-max hidden drop-shadow-[0_0_10px_rgba(0,0,0,0.15)] group-hover:block"
            style={{ transform: `translateX(${boxTranslate})` }}
        >
            <svg
                className="absolute -top-5 -translate-x-1/2 text-white"
                style={{ left: arrowLeft }}
                xmlns="http://www.w3.org/2000/svg"
                width="24"
                height="24"
                viewBox="0 0 24 24"
                fill="currentColor"
                stroke="currentColor"
                strokeWidth="1"
            >
                <path d="M13.73 4a2 2 0 0 0-3.46 0l-8 14A2 2 0 0 0 4 21h16a2 2 0 0 0 1.73-3Z" />
            </svg>
            <div className="flex bg-white px-6 pb-5 pt-2.5 items-center gap-3 rounded-lg">
                <div className="flex flex-col items-center gap-1">
                    <span className="text-gray-900 text-sm font-semibold p-2.5">
                        사이즈
                    </span>
                    <div className="flex flex-col gap-2 text-gray-600 text-sm">
                        <span>S</span>
                        <span>M</span>
                        <span>L</span>
                    </div>
                </div>
                <div className="flex flex-col items-center gap-1">
                    <span className="text-gray-900 text-sm font-semibold p-2.5">
                        크기 (가로+세로+높이)
                    </span>
                    <div className="flex flex-col gap-2 text-gray-600 text-sm">
                        <span>40 미만</span>
                        <span>40~70</span>
                        <span>70~100</span>
                    </div>
                </div>
                <div className="flex flex-col items-center gap-1">
                    <span className="text-gray-900 text-sm font-semibold p-2.5">
                        무게
                    </span>
                    <div className="flex flex-col gap-2 text-gray-600 text-sm">
                        <span>500g 미만</span>
                        <span>500g~1.5kg</span>
                        <span>1.5kg~3kg</span>
                    </div>
                </div>
            </div>
        </div>
    );
};
