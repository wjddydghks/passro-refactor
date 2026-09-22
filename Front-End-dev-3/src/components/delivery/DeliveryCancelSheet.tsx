import BottomSheet from "../common/BottomSheet";

interface DeliveryCancelSheetProps {
    isSubmitting: boolean;
    errorMessage?: string | null;
    onClose: () => void;
    onConfirm: () => void;
}

export function DeliveryCancelSheet({
    isSubmitting,
    errorMessage,
    onClose,
    onConfirm,
}: DeliveryCancelSheetProps) {
    return (
        <BottomSheet
            title="전달 요청을 취소할까요?"
            onClose={isSubmitting ? undefined : onClose}
            footer={
                <div className="grid grid-cols-2 gap-2.5">
                    <button
                        type="button"
                        onClick={onClose}
                        disabled={isSubmitting}
                        className="rounded-xl bg-gray-100 py-3.5 font-bold text-gray-700 disabled:text-gray-400"
                    >
                        돌아가기
                    </button>
                    <button
                        type="button"
                        onClick={onConfirm}
                        disabled={isSubmitting}
                        className="rounded-xl bg-[#FF3D3D] py-3.5 font-bold text-white disabled:bg-rose-300"
                    >
                        {isSubmitting ? "취소 처리 중..." : "요청 취소하기"}
                    </button>
                </div>
            }
        >
            <p className="text-sm font-medium leading-6 text-gray-600">
                매칭 대기 중인 요청만 취소할 수 있습니다. 취소한 요청은 다시
                진행할 수 없습니다.
            </p>
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
