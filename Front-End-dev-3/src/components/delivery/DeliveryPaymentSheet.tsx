import { memo } from "react";
import BottomSheet from "../common/BottomSheet";
import { DeliveryPayment } from "../../types/delivery/sender";

interface DeliveryPaymentSheetProps {
    payment: DeliveryPayment;
    isConfirming?: boolean;
    errorMessage?: string;
    onConfirm?: () => void;
    onClose?: () => void;
}

function formatPoint(value: number, prefix = "") {
    return `${prefix}${value.toLocaleString("ko-KR")} P`;
}

function DeliveryPaymentSheet({
    payment,
    isConfirming = false,
    errorMessage,
    onConfirm,
    onClose,
}: DeliveryPaymentSheetProps) {
    const breakdownItems = [
        { label: "기본 요금", value: formatPoint(payment.basePoint) },
        { label: "거리 요금", value: formatPoint(payment.distancePoint, "+ ") },
        { label: "무게 요금", value: formatPoint(payment.weightPoint, "+ ") },
    ];

    return (
        <BottomSheet
            title="정산 금액"
            titleAlign="left"
            onClose={isConfirming ? undefined : onClose}
            footer={
                <div>
                    {errorMessage ? (
                        <p
                            className="mb-3 text-sm font-medium text-rose-600"
                            role="alert"
                        >
                            {errorMessage}
                        </p>
                    ) : null}
                    <button
                        type="button"
                        onClick={onConfirm}
                        disabled={isConfirming}
                        className="flex w-full items-center justify-center rounded-lg bg-gray-800 py-3.5 font-bold text-white transition hover:bg-gray-900 focus:outline-none disabled:cursor-not-allowed disabled:bg-gray-300"
                    >
                        {isConfirming ? "결제 처리 중..." : "확인"}
                    </button>
                </div>
            }
        >
            <div className="px-[10px]">
                <div className="flex flex-col gap-2.5">
                    {breakdownItems.map((item) => (
                        <div
                            key={item.label}
                            className="flex items-center justify-between"
                        >
                            <span className="text-gray-400">{item.label}</span>
                            <span className="text-lg font-semibold text-gray-600">
                                {item.value}
                            </span>
                        </div>
                    ))}
                </div>

                <div className="my-4 h-px bg-gray-100" aria-hidden="true" />

                <div className="flex items-center justify-between">
                    <span className="text-xl font-bold text-gray-800">
                        최종 정산 금액
                    </span>
                    <span className="text-2xl font-bold text-purple-600">
                        {formatPoint(payment.totalPoint)}
                    </span>
                </div>
            </div>
        </BottomSheet>
    );
}

export default memo(DeliveryPaymentSheet);
