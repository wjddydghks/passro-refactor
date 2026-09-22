import { useNavigate } from "react-router-dom";
import PageHeader from "../components/common/PageHeader";
import { useCallback, useEffect, useMemo } from "react";
import { chatApi } from "../apis";
import { useApiRequest } from "../hooks/useApiRequest";
import type { ChatRoom } from "../data/chatRooms";
import ChatAvatar from "../components/chat/ChatAvatar";
import { formatChatRoomDateTime } from "../utils/chatDateTime";
import noChatImage from "../assets/images/chat/no-chat.svg";


export default function ChatListPage() {
    const navigate = useNavigate();

    const loadChatList = useCallback(() => {
        return chatApi.getRooms();
    }, []);

    const { data, error, isLoading, execute } = useApiRequest(loadChatList);

    useEffect(() => {
        let isStopped = false;
        let timerId: number;

        const pollChatRooms = async () => {
            try {
                await execute();
            } catch {
                // 일시적인 폴링 실패는 다음 주기에 다시 시도한다.
            } finally {
                if (!isStopped) {
                    timerId = window.setTimeout(pollChatRooms, 1_000);
                }
            }
        };

        void pollChatRooms();

        return () => {
            isStopped = true;
            window.clearTimeout(timerId);
        };
    }, [execute]);

    const chatRooms = useMemo<ChatRoom[]>(
        () =>
            (data ?? []).flatMap((datum) => {
                const lastMessage = datum.lastMessage?.trim();
                if (!lastMessage) {
                    return [];
                }

                return [
                    {
                        id: datum.deliveryId.toString(),
                        participantName: datum.partner.nickname,
                        participantPicture: datum.partner.picture,
                        itemName: datum.itemName ?? "전달 물품",
                        route: "",
                        status: "",
                        lastMessage,
                        updatedAt: formatChatRoomDateTime(datum.lastMessageAt),
                        unreadCount: datum.unreadCount,
                        messages: [],
                    },
                ];
            }),
        [data],
    );

    return (
        <main className="page-container flex h-full min-h-0 flex-col overflow-hidden px-0">
            <PageHeader
                title="채팅"
                className="shrink-0 px-4"
            />

            <div className="scrollbar-hidden min-h-0 flex-1 overflow-y-auto pt-5">
                {isLoading && data === null ? (
                    <p
                        className="py-16 text-center text-sm font-medium text-gray-400"
                        role="status"
                    >
                    </p>
                ) : data !== null && chatRooms.length === 0 ? (
                    <div className="w-full h-full flex justify-center items-center">
                        <div>
                            <div className="w-full flex justify-center flex-row">
                                <img src={noChatImage} alt="" />
                            </div>
                            <p className="mt-[33px] text-center text-sm font-medium text-gray-400">
                                아직 시작된 채팅이 없어요!
                            </p>
                        </div>
                    </div>
                ) : (
                    <ul className="divide-y divide-gray-100">
                        {chatRooms.map((room) => (
                            <li key={room.id}>
                                <button
                                    type="button"
                                    onClick={() =>
                                        navigate(`/delivery/chat/${room.id}`)
                                    }
                                    className="flex w-full items-center gap-4 px-4 py-5 text-left transition-colors hover:bg-gray-50 active:bg-gray-100 focus:outline-none"
                                    aria-label={`${room.participantName}님과의 채팅 열기`}
                                >
                                    <ChatAvatar
                                        name={room.participantName}
                                        picture={room.participantPicture}
                                    />

                                    <span className="min-w-0 flex-1">
                                        <span className="flex items-center gap-2">
                                            <strong className="truncate text-[16px] text-gray-900">
                                                {room.participantName}
                                            </strong>
                                            <span className="truncate text-xs font-medium text-gray-400">
                                                {room.itemName}
                                            </span>
                                        </span>
                                        <span className="mt-1 block truncate text-sm text-gray-500">
                                            {room.lastMessage}
                                        </span>
                                    </span>

                                    <span className="flex shrink-0 flex-col items-end gap-2">
                                        {room.updatedAt ? (
                                            <time className="text-xs text-gray-400">
                                                {room.updatedAt}
                                            </time>
                                        ) : null}
                                        {room.unreadCount > 0 ? (
                                            <span className="flex h-5 min-w-5 items-center justify-center rounded-full bg-purple-500 px-1.5 text-[11px] font-bold text-white">
                                                {room.unreadCount}
                                            </span>
                                        ) : (
                                            <span
                                                className="h-5"
                                                aria-hidden="true"
                                            />
                                        )}
                                    </span>
                                </button>
                            </li>
                        ))}
                    </ul>
                )}
            </div>
        </main>
    );
}
