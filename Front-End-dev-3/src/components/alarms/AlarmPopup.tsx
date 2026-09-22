import { Fragment } from "react";
import divider from "../../assets/icons/alarm-divider.svg";
import type { NotificationItem } from "../../apis/notificationApi";

interface AlarmPopupProps {
    alarms: NotificationItem[];
    onClearAll: () => void;
    onSelect: (alarm: NotificationItem) => void;
    className?: string;
    isLoading?: boolean;
    errorMessage?: string;
}

function formatAlarmTime(createdAt: string) {
    const createdDate = new Date(createdAt);

    if (Number.isNaN(createdDate.getTime())) {
        return createdAt;
    }

    const now = new Date();
    const elapsedMinutes = Math.floor(
        (now.getTime() - createdDate.getTime()) / 60_000,
    );

    if (elapsedMinutes < 1) {
        return "방금";
    }

    if (elapsedMinutes < 60) {
        return `${elapsedMinutes}분전`;
    }

    const month = String(createdDate.getMonth() + 1).padStart(2, "0");
    const day = String(createdDate.getDate()).padStart(2, "0");

    return `${month}-${day}`;
}

export default function AlarmPopup({
    alarms,
    onClearAll,
    onSelect,
    className = "",
    isLoading = false,
    errorMessage,
}: AlarmPopupProps) {
    return (
        <section
            className={`flex max-h-[70dvh] w-[286px] flex-col gap-[18px] rounded-[9px] bg-white p-[15px] shadow-[0_0_4px_rgba(0,0,0,0.1)] ${className}`}
            aria-label="알림 목록"
            role="dialog"
        >
            <div className="flex items-center justify-between text-xs font-semibold leading-[14px]">
                <h2 className="text-gray-600">알림</h2>
                <button
                    type="button"
                    onClick={onClearAll}
                    disabled={alarms.length === 0}
                    className="text-gray-300 transition-colors hover:text-gray-500 focus:outline-none focus-visible:ring-2 focus-visible:ring-purple-500 disabled:cursor-default disabled:text-gray-200"
                >
                    모두 지우기
                </button>
            </div>

            {isLoading ? (
                <p className="py-4 text-center text-xs font-medium leading-[14px] text-gray-500">
                    알림을 불러오는 중입니다.
                </p>
            ) : errorMessage ? (
                <p
                    className="py-4 text-center text-xs font-medium leading-[14px] text-red-500"
                    role="alert"
                >
                    {errorMessage}
                </p>
            ) : alarms.length === 0 ? (
                <p className="py-4 text-center text-xs font-medium leading-[14px] text-gray-500">
                    새로운 알림이 없습니다.
                </p>
            ) : (
                <ul className="scrollbar-hidden flex min-h-0 flex-col gap-[15px] overflow-y-auto">
                    {alarms.map((alarm, index) => (
                        <Fragment key={alarm.notificationId}>
                            {index > 0 ? (
                                <li aria-hidden="true">
                                    <img
                                        src={divider}
                                        alt=""
                                        className="h-px w-full"
                                    />
                                </li>
                            ) : null}

                            <li>
                                <button
                                    type="button"
                                    onClick={() => onSelect(alarm)}
                                    className="flex w-full flex-col gap-[6px] text-left focus:outline-none focus-visible:ring-2 focus-visible:ring-purple-500"
                                    aria-label={`${alarm.read ? "읽은" : "읽지 않은"} 알림: ${alarm.title}`}
                                >
                                    <div className="flex w-full items-center justify-between gap-3">
                                        <h3
                                            className={`min-w-0 truncate text-[13px] leading-[14px] transition-colors ${
                                                alarm.read
                                                    ? "font-medium text-gray-500"
                                                    : "font-bold text-gray-900"
                                            }`}
                                        >
                                            {alarm.title}
                                        </h3>
                                        <time
                                            dateTime={alarm.createdAt}
                                            className={`shrink-0 text-[10px] font-medium leading-[14px] transition-colors ${
                                                alarm.read
                                                    ? "text-gray-400"
                                                    : "text-gray-600"
                                            }`}
                                        >
                                            {formatAlarmTime(alarm.createdAt)}
                                        </time>
                                    </div>
                                    <p
                                        className={`break-words text-xs leading-[14px] transition-colors ${
                                            alarm.read
                                                ? "font-normal text-gray-400"
                                                : "font-semibold text-gray-700"
                                        }`}
                                    >
                                        {alarm.content}
                                    </p>
                                </button>
                            </li>
                        </Fragment>
                    ))}
                </ul>
            )}
        </section>
    );
}
