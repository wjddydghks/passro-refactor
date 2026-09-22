import MessageIcon from "../../assets/icons/MessageIcon";

interface DeliveryPersonCardProps {
    name: string | null;
    picture?: string | null;
    place?: string | null;
    emptyMessage?: string;
    actionLabel?: string;
    onAction?: () => void;
    onMessageClick?: () => void;
}

export const DeliveryPersonCard = ({
    name,
    picture,
    place,
    emptyMessage = "전달자 정보가 없습니다.",
    actionLabel,
    onAction,
    onMessageClick,
}: DeliveryPersonCardProps) => {
    if (!name) {
        return (
            <div className="w-full rounded-xl bg-gray-50 px-5 py-5 text-center text-sm font-medium text-gray-500">
                {emptyMessage}
            </div>
        );
    }

    return (
        <div className="flex w-full items-center justify-between rounded-xl bg-gray-50 px-5 py-4">
            <div className="flex min-w-0 items-center gap-4">
                {picture ? (
                    <img
                        src={picture}
                        alt=""
                        className="h-10 w-10 shrink-0 rounded-full object-cover"
                    />
                ) : (
                    <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-gray-800 text-sm font-bold text-white">
                        {name.trim().charAt(0)}
                    </div>
                )}
                <div className="flex min-w-0 flex-col">
                    <span className="truncate text-lg font-bold">{name}</span>
                    {actionLabel && onAction ? (
                        <button
                            type="button"
                            onClick={onAction}
                            className="truncate text-xs text-gray-500"
                        >
                            {actionLabel}
                        </button>
                    ) : place ? (
                        <span className="truncate text-xs text-gray-500">
                            {place}
                        </span>
                    ) : null}
                </div>
            </div>
            {onMessageClick ? (
                <button
                    type="button"
                    onClick={onMessageClick}
                    aria-label={`${name}님과의 채팅 열기`}
                    className="shrink-0 rounded-full p-1 text-gray-400 transition hover:bg-gray-100 hover:text-gray-600 focus:outline-none"
                >
                    <MessageIcon />
                </button>
            ) : (
                <MessageIcon className="shrink-0 text-gray-400" />
            )}
        </div>
    );
};
