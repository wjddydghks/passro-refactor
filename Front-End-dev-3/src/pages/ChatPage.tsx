import {
    useCallback,
    useEffect,
    useRef,
    useState,
    type FormEvent,
} from "react";
import { useNavigate, useParams } from "react-router-dom";
import {
    chatApi,
    reportApi,
    senderDeliveryApi,
    shipperDeliveryApi,
} from "../apis";
import type { ChatMessage } from "../apis/chatApi";
// import { senderDeliveryApi } from "../apis/delivery/senderDeliveryApi";
// import { shipperDeliveryApi } from "../apis/delivery/shipperDeliveryApi";
import PageHeader from "../components/common/PageHeader";
import { useApiRequest } from "../hooks/useApiRequest";
import { ApiError } from "../types/api";
import { getDeliveryStatusLabel } from "../utils/deliveryStatus";
import ChatModal from "../components/chat/ChatModal";
import ChatRoomSkeleton from "../components/chat/ChatRoomSkeleton";
import ChatAvatar from "../components/chat/ChatAvatar";
import ChatMessageImage from "../components/chat/ChatMessageImage";
import { notifyChatRead } from "../utils/chatEvents";
import { formatChatTime, parseChatDateTime } from "../utils/chatDateTime";

const CHAT_DATE_FORMATTER = new Intl.DateTimeFormat("ko-KR", {
    year: "numeric",
    month: "long",
    day: "numeric",
    timeZone: "Asia/Seoul",
});

const CHAT_DATE_KEY_FORMATTER = new Intl.DateTimeFormat("en-CA", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    timeZone: "Asia/Seoul",
});

function getChatDateKey(createdAt: string) {
    const date = parseChatDateTime(createdAt);
    return Number.isNaN(date.getTime())
        ? ""
        : CHAT_DATE_KEY_FORMATTER.format(date);
}

function formatChatDate(createdAt: string) {
    const date = parseChatDateTime(createdAt);
    return Number.isNaN(date.getTime()) ? "" : CHAT_DATE_FORMATTER.format(date);
}

export default function ChatPage() {
    const navigate = useNavigate();
    const { chatRoomId } = useParams<{ chatRoomId: string }>();
    const messageEndRef = useRef<HTMLDivElement>(null);
    const messageInputRef = useRef<HTMLInputElement>(null);
    const latestMessageIdRef = useRef<number | undefined>(undefined);
    const messagesRef = useRef<ChatMessage[]>([]);
    const [messages, setMessages] = useState<ChatMessage[]>([]);
    const [draft, setDraft] = useState("");
    const [isSending, setIsSending] = useState(false);
    const [sendError, setSendError] = useState("");
    const [isOpen, setIsopen] = useState(false);
    const [isReportOpen, setIsReportOpen] = useState(false);
    const [isReporting, setIsReporting] = useState(false);
    const [reportError, setReportError] = useState("");

    const mergeMessages = useCallback((nextMessages: ChatMessage[]) => {
        if (nextMessages.length === 0) {
            return;
        }

        latestMessageIdRef.current = Math.max(
            latestMessageIdRef.current ?? 0,
            ...nextMessages.map((message) => message.id),
        );

        setMessages((previousMessages) => {
            const messageMap = new Map(
                previousMessages.map((message) => [message.id, message]),
            );

            nextMessages.forEach((message) => {
                messageMap.set(message.id, message);
            });

            const mergedMessages = [...messageMap.values()].sort(
                (a, b) => a.id - b.id,
            );
            messagesRef.current = mergedMessages;
            return mergedMessages;
        });
    }, []);

    const loadChatRoom = useCallback(async () => {
        const deliveryId = Number(chatRoomId);

        if (!Number.isInteger(deliveryId) || deliveryId <= 0) {
            throw new ApiError({ message: "올바르지 않은 채팅방입니다." });
        }

        const [messages, roomInfo, senderDeliveries, shipperDeliveries] =
            await Promise.all([
                chatApi.getMessages(deliveryId),
                chatApi.getRoomInfo(deliveryId),
                senderDeliveryApi.getDeliveryList().catch(() => []),
                shipperDeliveryApi.getDeliveryList().catch(() => []),
            ]);

        notifyChatRead();

        const deliveryRole = senderDeliveries.some(
            (delivery) => delivery.deliveryId === deliveryId,
        )
            ? "sender"
            : shipperDeliveries.some((delivery) => delivery.id === deliveryId)
              ? "shipper"
              : null;

        return { messages, roomInfo, deliveryRole };
    }, [chatRoomId]);

    const { data, error, isLoading, execute } = useApiRequest(loadChatRoom);

    useEffect(() => {
        void execute().catch(() => undefined);
    }, [execute]);

    useEffect(() => {
        if (!data) {
            return;
        }

        const initialMessages = [...data.messages].sort((a, b) => a.id - b.id);
        messagesRef.current = initialMessages;
        setMessages(initialMessages);
        latestMessageIdRef.current = initialMessages.at(-1)?.id;
    }, [data]);

    useEffect(() => {
        if (!data) {
            return;
        }

        const deliveryId = Number(chatRoomId);
        if (!Number.isInteger(deliveryId) || deliveryId <= 0) {
            return;
        }

        let isStopped = false;
        let timerId: number;

        const pollMessages = async () => {
            try {
                const oldestUnreadMessageIndex = messagesRef.current.findIndex(
                    (message) =>
                        !message.isRead &&
                        message.senderNickname !==
                            data.roomInfo.partnerNickname,
                );
                const pollingAfterId =
                    oldestUnreadMessageIndex >= 0
                        ? messagesRef.current[oldestUnreadMessageIndex - 1]?.id
                        : latestMessageIdRef.current;
                const nextMessages = await chatApi.getMessages(
                    deliveryId,
                    pollingAfterId,
                );

                if (!isStopped) {
                    if (
                        nextMessages.some(
                            (message) =>
                                message.senderNickname ===
                                data.roomInfo.partnerNickname,
                        )
                    ) {
                        notifyChatRead();
                    }
                    mergeMessages(nextMessages);
                }
            } catch {
                // 일시적인 폴링 실패는 다음 주기에 다시 시도한다.
            } finally {
                if (!isStopped) {
                    timerId = window.setTimeout(pollMessages, 1_000);
                }
            }
        };

        timerId = window.setTimeout(pollMessages, 1_000);

        return () => {
            isStopped = true;
            window.clearTimeout(timerId);
        };
    }, [chatRoomId, data, mergeMessages]);

    useEffect(() => {
        if (messages.length) {
            messageEndRef.current?.scrollIntoView({ block: "end" });
        }
    }, [messages.length]);

    const handleSendMessage = async (event: FormEvent<HTMLFormElement>) => {
        event.preventDefault();

        messageInputRef.current?.focus({ preventScroll: true });

        const content = draft.trim();
        const deliveryId = Number(chatRoomId);
        if (
            !content ||
            isSending ||
            !Number.isInteger(deliveryId) ||
            deliveryId <= 0
        ) {
            return;
        }

        setIsSending(true);
        setSendError("");
        setDraft("");

        try {
            const sentMessage = await chatApi.sendMessage(deliveryId, {
                content,
            });
            mergeMessages([sentMessage]);
        } catch (caughtError) {
            setDraft((currentDraft) => currentDraft || content);
            setSendError(
                caughtError instanceof ApiError
                    ? caughtError.message
                    : "메시지를 전송하지 못했습니다.",
            );
        } finally {
            setIsSending(false);
        }

        event.preventDefault();
    };

    const handleOpenReport = () => {
        setIsopen(false);
        setReportError("");
        setIsReportOpen(true);
    };

    const handleExitSubmit = async () => {
        const deliveryId = Number(chatRoomId);

        if (!Number.isInteger(deliveryId) || deliveryId <= 0) {
            return;
        }

        setIsopen(false);
        try {
            await chatApi.exitRoom(deliveryId);
            navigate("/delivery/chat");
        } catch (caughtError) {
            setReportError(
                caughtError instanceof ApiError
                    ? caughtError.message
                    : "나가지 못했습니다. 다시 시도해주세요.",
            );
        } finally {
            setIsReporting(false);
        }
    };

    if (isLoading || (!data && !error)) {
        return <ChatRoomSkeleton />;
    }

    if (error || !data) {
        return (
            <div className="flex h-full flex-col bg-white">
                <PageHeader
                    title="채팅"
                    onBack={() => navigate(-1)}
                    className="shrink-0 px-4 pt-3"
                />
                <div className="flex flex-1 flex-col items-center justify-center gap-4 px-6 text-center">
                    <p className="text-sm text-red-500">
                        {error?.message ?? "채팅을 불러오지 못했습니다."}
                    </p>
                    <button
                        type="button"
                        onClick={() => void execute().catch(() => undefined)}
                        className="rounded-lg bg-purple-600 px-4 py-2 text-sm font-semibold text-white"
                    >
                        다시 시도
                    </button>
                </div>
            </div>
        );
    }

    const { roomInfo, deliveryRole } = data;
    const deliveryId = Number(chatRoomId);
    const route = [roomInfo.departure, roomInfo.arrival]
        .filter((station): station is string => Boolean(station))
        .join(" → ");

    const handleDeliveryNavigate = () => {
        if (deliveryRole === "sender") {
            navigate(`/delivery/status/${deliveryId}`);
        }

        if (deliveryRole === "shipper") {
            navigate(`/delivery/tracking/${deliveryId}`);
        }
    };

    return (
        <div className="relative flex h-full min-h-0 w-full flex-col overflow-hidden bg-white">
            {/* 상단바 영역 */}
            <div className="z-40 w-full shrink-0 border-b border-gray-100 bg-white px-4 pb-4 pt-3">
                <PageHeader
                    title={
                        <span className="flex items-center gap-2.5">
                            <ChatAvatar
                                name={roomInfo.partnerNickname}
                                picture={roomInfo.partnerPicture}
                                className="h-8 w-8 text-sm"
                            />
                            <span>{roomInfo.partnerNickname}</span>
                        </span>
                    }
                    onBack={() => navigate(-1)}
                    className="mb-3"
                    rightAction={
                        <button
                            type="button"
                            onClick={() => setIsopen(true)}
                            className="-mr-1 p-1 text-gray-600 focus:outline-none"
                            aria-label="채팅방 메뉴 열기"
                        >
                            <svg
                                xmlns="http://www.w3.org/2000/svg"
                                fill="none"
                                viewBox="0 0 24 24"
                                strokeWidth={2.5}
                                stroke="currentColor"
                                className="h-6 w-6"
                            >
                                <path
                                    strokeLinecap="round"
                                    strokeLinejoin="round"
                                    d="M6.75 12a.75.75 0 11-1.5 0 .75.75 0 011.5 0zM12.75 12a.75.75 0 11-1.5 0 .75.75 0 011.5 0zM18.75 12a.75.75 0 11-1.5 0 .75.75 0 011.5 0z"
                                />
                            </svg>
                        </button>
                    }
                />

                <button
                    type="button"
                    onClick={handleDeliveryNavigate}
                    disabled={!deliveryRole}
                    className="flex w-full items-center justify-between rounded-2xl bg-[#F7F7F9] p-4 text-left transition-colors enabled:hover:bg-gray-100 disabled:cursor-default"
                    aria-label={
                        deliveryRole
                            ? `${roomInfo.itemName ?? "전달 물품"} 상세 보기`
                            : "전달 물품 정보"
                    }
                >
                    <div className="min-w-0">
                        <h2 className="mb-0.5 truncate text-[15px] font-bold text-gray-900">
                            {roomInfo.itemName ?? "전달 물품"}
                        </h2>
                        {route ? (
                            <p className="truncate text-sm font-semibold text-gray-400">
                                {route}
                            </p>
                        ) : null}
                    </div>
                    <span className="ml-3 shrink-0 rounded-xl bg-[#EBEBFF] px-3 py-1.5 text-xs font-semibold text-[#6366F1]">
                        {getDeliveryStatusLabel(roomInfo.deliveryStatus)}
                    </span>
                </button>
            </div>

            <div className="scrollbar-hidden min-h-0 flex-1 space-y-[10px] overflow-y-auto bg-white px-4">
                {messages.length === 0 ? (
                    <p className="py-12 text-center text-sm text-gray-400">
                        아직 주고받은 메시지가 없습니다.
                    </p>
                ) : (
                    messages.map((message, index) => {
                        const isMine =
                            message.senderNickname !== roomInfo.partnerNickname;
                        const hasContent = Boolean(message.content.trim());
                        const timestamp = formatChatTime(message.createdAt);
                        const currentDateKey = getChatDateKey(
                            message.createdAt,
                        );
                        const previousDateKey =
                            index > 0
                                ? getChatDateKey(messages[index - 1].createdAt)
                                : "";
                        const shouldShowDateDivider =
                            Boolean(currentDateKey) &&
                            currentDateKey !== previousDateKey;

                        return (
                            <div key={message.id}>
                                {shouldShowDateDivider ? (
                                    <div className="flex items-center gap-3 py-5">
                                        <span
                                            className="h-px flex-1 bg-gray-100"
                                            aria-hidden="true"
                                        />
                                        <time
                                            dateTime={message.createdAt}
                                            className="shrink-0 text-xs font-medium text-gray-400"
                                        >
                                            {formatChatDate(message.createdAt)}
                                        </time>
                                        <span
                                            className="h-px flex-1 bg-gray-100"
                                            aria-hidden="true"
                                        />
                                    </div>
                                ) : null}

                                {message.systemMessage ? (
                                    <div className="flex w-full flex-col items-start gap-2">
                                        {message.imageKey ? (
                                            <ChatMessageImage
                                                imageKey={message.imageKey}
                                                alt="시스템 메시지 이미지"
                                                borderRadiusClassName="rounded-[30px]"
                                            />
                                        ) : null}
                                        {hasContent ? (
                                            <p className="w-full rounded-[30px] border border-gray-100 bg-white px-4 py-2 text-left text-[12px] font-medium leading-4 text-gray-500">
                                                {message.content}
                                            </p>
                                        ) : null}
                                    </div>
                                ) : (
                                    <div
                                        className={`flex ${isMine ? "justify-end" : "justify-start"}`}
                                    >
                                        {!isMine ? (
                                        <ChatAvatar
                                            name={roomInfo.partnerNickname}
                                            picture={roomInfo.partnerPicture}
                                            className="mr-2 mt-1 h-8 w-8 text-xs"
                                        />
                                        ) : null}
                                        <div
                                            className={`flex w-full items-end gap-[5px] ${
                                                isMine
                                                    ? "justify-end"
                                                    : "justify-start"
                                            }`}
                                        >
                                        <div className="flex flex-col mb gap-y-[2px]">
                                            {isMine && !message.isRead ? (
                                                <span className="text-right text-[12px] font-medium text-gray-400">
                                                    읽지 않음
                                                </span>
                                            ) : null}

                                            {isMine && timestamp ? (
                                                <div className="flex">
                                                    <time
                                                        dateTime={
                                                            message.createdAt
                                                        }
                                                        className="shrink-0 whitespace-nowrap text-[12px] font-medium text-gray-400"
                                                    >
                                                        {timestamp}
                                                    </time>
                                                </div>
                                            ) : null}
                                        </div>

                                        <div
                                            className={`flex max-w-[75%] flex-col gap-1.5 ${
                                                isMine
                                                    ? "items-end"
                                                    : "items-start"
                                            }`}
                                        >
                                            {message.imageKey ? (
                                                <ChatMessageImage
                                                    imageKey={message.imageKey}
                                                    alt={`${message.senderNickname}님이 보낸 이미지`}
                                                />
                                            ) : null}
                                            {hasContent ? (
                                                <div
                                                    className={`rounded-3xl px-4 py-3 text-[15px] font-semibold leading-relaxed ${
                                                        isMine
                                                            ? "bg-[#6366F1] text-white"
                                                            : "bg-[#EFEFEF] text-gray-800"
                                                    }`}
                                                >
                                                    {message.content}
                                                </div>
                                            ) : null}
                                        </div>

                                        {!isMine && timestamp ? (
                                            <time
                                                dateTime={message.createdAt}
                                                className="mb-1 shrink-0 whitespace-nowrap text-[11px] font-normal text-gray-400"
                                            >
                                                {timestamp}
                                            </time>
                                        ) : null}
                                        </div>
                                    </div>
                                )}
                            </div>
                        );
                    })
                )}
                <div ref={messageEndRef} />
            </div>

            <div className="w-full shrink-0 border-gray-100 bg-white px-4 py-4">
                {sendError ? (
                    <p className="mb-2 px-2 text-xs text-red-500">
                        {sendError}
                    </p>
                ) : null}
                <form
                    onSubmit={handleSendMessage}
                    className="flex items-center rounded-[24px] bg-[#F7F7F9] px-4 py-2"
                >
                    <input
                        ref={messageInputRef}
                        type="text"
                        placeholder="채팅을 입력하세요"
                        value={draft}
                        onChange={(event) => {
                            setDraft(event.target.value);
                            if (sendError) {
                                setSendError("");
                            }
                        }}
                        className="flex-1 bg-transparent font-medium text-[15px] text-gray-800 placeholder-gray-400 focus:outline-none"
                    />
                    <button
                        type="submit"
                        disabled={!draft.trim() || isSending}
                        onPointerDown={(event) => event.preventDefault()}
                        className="ml-2 flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-[#6366F1] text-white disabled:bg-[#C2C2C9]"
                        aria-label="메시지 전송"
                    >
                        <svg
                            xmlns="http://www.w3.org/2000/svg"
                            fill="none"
                            viewBox="0 0 24 24"
                            strokeWidth={3}
                            stroke="currentColor"
                            className="h-4 w-4"
                        >
                            <path
                                strokeLinecap="round"
                                strokeLinejoin="round"
                                d="M4.5 10.5L12 3m0 0l7.5 7.5M12 3v18"
                            />
                        </svg>
                    </button>
                </form>
            </div>
            {isOpen && (
                <ChatModal
                    onClose={() => setIsopen(false)}
                    onExit={handleExitSubmit}
                    onReport={handleOpenReport}
                    chatMessageId={Number(chatRoomId)}
                />
            )}
        </div>
    );
}
