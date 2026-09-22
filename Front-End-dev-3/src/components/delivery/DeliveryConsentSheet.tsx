import { memo, useState } from "react";
import BottomSheet from "../common/BottomSheet";

const CONSENT_ITEMS = [
    "맡기는 물품은 전달 가능한 물품입니다.",
    "물품 정보를 정확하게 입력했습니다.",
    "운송 중 문제가 없도록 포장했습니다.",
    "수령인의 정보를 정확하게 입력했습니다.",
    "전달 제한 품목 및 보상 정책을 확인했습니다.",
    "내용을 확인하고 전달을 요청합니다.",
];

interface DeliveryConsentSheetProps {
    onConfirm?: () => void;
    onClose?: () => void;
}

function DeliveryConsentSheet({
    onConfirm,
    onClose,
}: DeliveryConsentSheetProps) {
    const [agreed, setAgreed] = useState(false);

    return (
        <BottomSheet
            title="[필수] 전달 요청 전 확인 및 동의"
            onClose={onClose}
            footer={
                <button
                    type="button"
                    onClick={onConfirm}
                    disabled={!agreed}
                    className="flex w-full py-3.5 items-center justify-center rounded-lg bg-gray-800 font-bold text-white transition hover:bg-gray-900 focus:outline-none disabled:cursor-not-allowed disabled:bg-gray-200 disabled:text-gray-500 disabled:hover:bg-gray-300"
                >
                    전달 등록하기
                </button>
            }
        >
            <ul className="flex flex-col gap-1 text-[14px] font-normal leading-[22px] text-[#70727E]">
                {CONSENT_ITEMS.map((item) => (
                    <li key={item} className="flex gap-1">
                        <span aria-hidden="true">•</span>
                        <span>{item}</span>
                    </li>
                ))}
            </ul>

            <label className="mt-6 flex items-center gap-[10px]">
                <input
                    type="checkbox"
                    checked={agreed}
                    onChange={(event) => setAgreed(event.target.checked)}
                    className="h-5 w-5 rounded-[3px] border border-[#C6C8D2] accent-[#373840]"
                />
                <span className="text-[14px] font-medium leading-[22px] text-[#373840]">
                    확인 및 동의합니다.
                </span>
            </label>
        </BottomSheet>
    );
}

export default memo(DeliveryConsentSheet);
