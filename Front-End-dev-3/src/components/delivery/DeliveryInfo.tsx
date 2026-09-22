import { SizeInfo } from "./SizeInfo";

interface DeliveryInfoProps {
    itemName: string;
    itemPrice: string;
    itemSize: string;
    settlementPoint: string;
}

export const DeliveryInfo = ({
    itemName,
    itemPrice,
    itemSize,
    settlementPoint,
}: DeliveryInfoProps) => {
    return (
        <div className="flex flex-col">
            <div className="flex flex-col bg-gray-50 mt-20 p-5 rounded-lg">
                <div className="flex flex-col gap-5">
                    <span className="text-gray-500 text-lg font-bold">
                        요청 정보
                    </span>
                    <div className="flex flex-col gap-1">
                        <div className="flex w-full items-center justify-between">
                            <span className="text-gray-400 text-sm">
                                물품명
                            </span>
                            <span className="font-bold text-gray-600">
                                {itemName}
                            </span>
                        </div>
                        <div className="flex w-full items-center justify-between">
                            <span className="text-gray-400 text-sm">
                                물품 가액
                            </span>
                            <span className="font-bold text-gray-600">
                                {itemPrice}
                            </span>
                        </div>
                        <div className="flex w-full items-center justify-between">
                            <div className="flex items-center gap-1">
                                <span className="text-gray-400 text-sm">
                                    물품 크기
                                </span>
                                <div className="group relative flex items-center justify-center w-4 h-4 rounded-full bg-purple-400 text-white text-xs font-bold cursor-pointer">
                                    ?
                                    <SizeInfo
                                        boxTranslate="-24%"
                                        arrowLeft="24%"
                                    />
                                </div>
                            </div>
                            <span className="font-bold text-gray-600">
                                {itemSize}
                            </span>
                        </div>
                    </div>
                </div>
            </div>
            <div className="flex bg-gray-50 mt-8 p-5 rounded-lg justify-between items-center">
                <span className="text-gray-800 text-xl font-bold">
                    정산 포인트
                </span>
                <span className="font-bold text-purple-600 text-3xl">
                    {settlementPoint}
                </span>
            </div>
        </div>
    );
};
