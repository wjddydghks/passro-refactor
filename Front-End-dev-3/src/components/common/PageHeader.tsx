import type { ReactNode } from "react";
import ChevronIcon from "../../assets/icons/ChevronIcon";

interface PageHeaderProps {
    title: ReactNode;
    onBack?: () => void;
    rightAction?: ReactNode;
    className?: string;
}

export default function PageHeader({
    title,
    onBack,
    rightAction,
    className = "",
}: PageHeaderProps) {
    return (
        <header
            className={`flex h-12 items-center justify-between ${className}`.trim()}
        >
            {onBack ? (
                <button
                    type="button"
                    onClick={onBack}
                    className="-ml-1 p-1 text-gray-600 focus:outline-none"
                    aria-label="이전 페이지로 이동"
                >
                    <ChevronIcon />
                </button>
            ) : (
                <span className="h-8 w-8" aria-hidden="true" />
            )}

            <h1 className="text-xl font-bold text-gray-900">{title}</h1>

            {rightAction ?? (
                <span className="h-8 w-8" aria-hidden="true" />
            )}
        </header>
    );
}
