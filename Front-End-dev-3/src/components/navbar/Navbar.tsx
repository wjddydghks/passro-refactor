import { useCallback, useEffect, useState } from "react";
import { NavLink } from "react-router-dom";
import { chatApi } from "../../apis";
import { CHAT_READ_EVENT } from "../../utils/chatEvents";

function MarketIcon() {
    return (
        <svg
            viewBox="0 0 32 32"
            className="h-8 w-8"
            fill="none"
            aria-hidden="true"
        >
            <path
                d="M6 12h20l-1.4-5H7.4L6 12Z"
                stroke="currentColor"
                strokeWidth="2"
                strokeLinejoin="round"
            />
            <path
                d="M8 13v12h16V13M12 25v-6h8v6"
                stroke="currentColor"
                strokeWidth="2"
                strokeLinecap="round"
                strokeLinejoin="round"
            />
            <path
                d="M6 12c0 2 1.3 3 3 3s3-1 3-3c0 2 1.3 3 4 3s4-1 4-3c0 2 1.3 3 3 3s3-1 3-3"
                stroke="currentColor"
                strokeWidth="2"
                strokeLinecap="round"
            />
        </svg>
    );
}

export default function Navbar() {
    const [unreadChatCount, setUnreadChatCount] = useState(0);

    const loadUnreadChatCount = useCallback(async () => {
        try {
            const rooms = await chatApi.getRooms();
            setUnreadChatCount(
                rooms.reduce(
                    (total, room) => total + Math.max(0, room.unreadCount),
                    0,
                ),
            );
        } catch {
            // 일시적인 조회 실패 시 기존 배지 값을 유지한다.
        }
    }, []);

    useEffect(() => {
        let isStopped = false;
        let timerId: number;

        const pollUnreadCount = async () => {
            await loadUnreadChatCount();

            if (!isStopped) {
                timerId = window.setTimeout(pollUnreadCount, 3_000);
            }
        };

        void pollUnreadCount();
        window.addEventListener("focus", loadUnreadChatCount);
        window.addEventListener(CHAT_READ_EVENT, loadUnreadChatCount);

        return () => {
            isStopped = true;
            window.clearTimeout(timerId);
            window.removeEventListener("focus", loadUnreadChatCount);
            window.removeEventListener(CHAT_READ_EVENT, loadUnreadChatCount);
        };
    }, [loadUnreadChatCount]);

    const navList = [
        {
            id: 1,
            title: "홈",
            navigate: "/home",
            imgUrl: "/houseIcon.png",
        },
        {
            id: 2,
            title: "마켓",
            navigate: "/market",
            imgUrl: "/market.png",
        },
        {
            id: 3,
            title: "채팅",
            navigate: "/delivery/chat",
            imgUrl: "/messageIcon.png",
        },
        {
            id: 4,
            title: "마이페이지",
            navigate: "/mypage",
            imgUrl: "/userIcon.png",
        },
    ];

    return (
        <nav className="relative z-50 flex w-full shrink-0 items-center justify-center gap-10 bg-white p-1 pb-[max(0.25rem,env(safe-area-inset-bottom))] shadow-[0_-10px_32px_-8px_rgba(0,0,0,0.08)]">
            {navList.map((nav) => (
                <NavLink
                    key={nav.id}
                    to={nav.navigate}
                    title={nav.title}
                    className="group rounded p-3"
                >
                    {({ isActive }) => (
                        nav.imgUrl ? (
                            <span className="relative block">
                                <img
                                    src={nav.imgUrl}
                                    alt={nav.title}
                                    width="100"
                                    height="100"
                                    className={`h-8 w-8 object-contain transition-opacity hover:opacity-80 ${isActive
                                            ? "opacity-100"
                                            : "opacity-35 group-hover:opacity-60"
                                        }`}
                                />
                                {nav.id === 3 && unreadChatCount > 0 ? (
                                    <span
                                        className="absolute -right-2 -top-2 flex h-[18px] min-w-[18px] items-center justify-center rounded-full bg-errorRed px-1 text-[10px] font-bold leading-none text-white"
                                        aria-label={`읽지 않은 채팅 ${unreadChatCount}개`}
                                    >
                                        {unreadChatCount > 99
                                            ? "99+"
                                            : unreadChatCount}
                                    </span>
                                ) : null}
                            </span>
                        ) : (
                            <span
                                className={`block transition-opacity ${isActive
                                        ? "text-purple-600 opacity-100"
                                        : "text-gray-900 opacity-35 group-hover:opacity-60"
                                    }`}
                                aria-label={nav.title}
                            >
                                <MarketIcon />
                            </span>
                        )
                    )}
                </NavLink>
            ))}
        </nav>
    );
}
