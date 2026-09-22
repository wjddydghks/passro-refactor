type FeedbackModalProps = {
    title?: string;
    message: string;
    onClose: () => void;
    actionLabel?: string;
    onAction?: () => void;
    isActionDisabled?: boolean;
};

export default function FeedbackModal({
    title,
    message,
    onClose,
    actionLabel,
    onAction,
    isActionDisabled = false,
}: FeedbackModalProps) {
    if (!message) {
        return null;
    }

    return (
        <div
            className="fixed inset-0 z-[100] mx-auto flex w-full max-w-[402px] items-center justify-center overflow-y-auto bg-black/35 px-5 py-6 backdrop-blur-sm"
            role="dialog"
            aria-modal="true"
            onClick={onClose}
        >
            <div
                className="max-h-[calc(100dvh-48px)] w-full max-w-[340px] overflow-y-auto rounded-[14px] bg-white px-5 py-6 text-center shadow-lg sm:px-8"
                onClick={(event) => event.stopPropagation()}
            >
                {title ? (
                    <h2 className="text-lg font-bold text-gray-900">
                        {title}
                    </h2>
                ) : null}
                <p
                    className={`break-words [overflow-wrap:anywhere] ${title ? "mt-2 text-sm text-gray-600" : "font-semibold text-gray-900"}`}
                >
                    {message}
                </p>
                {actionLabel && onAction ? (
                    <div className="mt-5 grid grid-cols-2 gap-2.5">
                        <button
                            type="button"
                            onClick={onClose}
                            className="min-w-0 rounded-lg bg-gray-100 px-2 py-3.5 font-semibold text-gray-700 transition-colors hover:bg-gray-200"
                        >
                            닫기
                        </button>
                        <button
                            type="button"
                            onClick={onAction}
                            disabled={isActionDisabled}
                            className="min-w-0 rounded-lg bg-purple-500 px-2 py-3.5 font-semibold text-white transition-colors hover:bg-purple-600 disabled:cursor-not-allowed disabled:opacity-60"
                        >
                            {actionLabel}
                        </button>
                    </div>
                ) : (
                    <button
                        type="button"
                        onClick={onClose}
                        className="mt-5 w-full rounded-lg bg-purple-500 py-3.5 font-semibold text-white transition-colors hover:bg-purple-600"
                    >
                        확인
                    </button>
                )}
            </div>
        </div>
    );
}
