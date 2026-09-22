import { useState } from "react";
import BottomSheet from "../common/BottomSheet";

interface MissingDeliveryReportSheetProps {
    isSubmitting: boolean;
    errorMessage?: string | null;
    onClose: () => void;
    onSubmit: (content: string) => void;
}

const DEFAULT_REPORT_CONTENT =
    "전달 완료 요청을 받았지만 물건을 받지 못했습니다.";

export function MissingDeliveryReportSheet({
    isSubmitting,
    errorMessage,
    onClose,
    onSubmit,
}: MissingDeliveryReportSheetProps) {
    const [content, setContent] = useState(DEFAULT_REPORT_CONTENT);
    const trimmedContent = content.trim();

    return (
        <BottomSheet
            title="물건 미도착 신고"
            titleAlign="left"
            onClose={isSubmitting ? undefined : onClose}
            footer={
                <div className="grid grid-cols-2 gap-2.5">
                    <button
                        type="button"
                        onClick={onClose}
                        disabled={isSubmitting}
                        className="rounded-xl bg-gray-100 py-3.5 font-bold text-gray-700 disabled:text-gray-400"
                    >
                        닫기
                    </button>
                    <button
                        type="button"
                        onClick={() => onSubmit(trimmedContent)}
                        disabled={isSubmitting || !trimmedContent}
                        className="rounded-xl bg-rose-600 py-3.5 font-bold text-white disabled:bg-gray-200 disabled:text-gray-500"
                    >
                        {isSubmitting ? "접수 중..." : "미도착 신고하기"}
                    </button>
                </div>
            }
        >
            <p className="text-sm font-medium leading-6 text-gray-600">
                접수 내용은 해당 전달의 분실 문의로 기록됩니다. 현재 백엔드에서는
                신고 접수만 가능하며 전달 상태와 정산을 자동으로 보류하지는
                않습니다.
            </p>
            <label className="mt-5 block">
                <span className="text-sm font-bold text-gray-800">
                    미도착 상황
                </span>
                <textarea
                    value={content}
                    onChange={(event) => setContent(event.target.value)}
                    disabled={isSubmitting}
                    maxLength={1000}
                    className="mt-2 h-32 w-full resize-none rounded-xl bg-gray-50 px-4 py-3 text-sm font-medium leading-6 text-gray-800 outline-none focus:ring-2 focus:ring-purple-200 disabled:text-gray-400"
                    placeholder="물건을 받지 못한 상황을 입력해주세요."
                />
            </label>
            {errorMessage ? (
                <p
                    className="mt-4 rounded-xl bg-rose-50 px-4 py-3 text-sm font-semibold text-rose-700"
                    role="alert"
                >
                    {errorMessage}
                </p>
            ) : null}
        </BottomSheet>
    );
}
