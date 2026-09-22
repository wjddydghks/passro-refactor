import type { ReactNode } from "react";

interface BottomSheetProps {
    title: string;
    titleAlign?: "center" | "left";
    children: ReactNode;
    footer?: ReactNode;
    onClose?: () => void;
}

export default function BottomSheet({
    title,
    titleAlign = "center",
    children,
    footer,
    onClose,
}: BottomSheetProps) {
    return (
        <div
            className="bottom-sheet-backdrop-in fixed inset-0 z-[60] mx-auto w-full max-w-[402px] bg-black/20"
            onClick={onClose}
        >
            <div
                className="bottom-sheet-panel-in absolute inset-x-0 bottom-0 max-h-[90dvh] overflow-y-auto rounded-t-[30px] bg-white px-5 pb-8 pt-3"
                role="dialog"
                aria-modal="true"
                aria-labelledby="bottom-sheet-title"
                onClick={(event) => event.stopPropagation()}
            >
                <div
                    className="mx-auto h-1 w-[55px] rounded-full bg-gray-200"
                    aria-hidden="true"
                />
                <h2
                    id="bottom-sheet-title"
                    className={`pl-[10px] pr-[10px] mt-10 text-lg font-bold leading-[22px] text-gray-900 ${titleAlign === "center" ? "text-center" : "text-left"
                        }`}
                >
                    {title}
                </h2>
                <div className="mt-6">{children}</div>
                {footer ? <div className="mt-6">{footer}</div> : null}
            </div>
        </div>
    );
}
