import { useCallback, useEffect, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import { notificationApi } from "../../apis/notificationApi";
import type { NotificationItem } from "../../apis/notificationApi";
import type { HomeContent } from "../../types/home";
import type { UserRole } from "../../types/user";
import AlarmPopup from "../alarms/AlarmPopup";
import DeliveryConsentSheet from "../delivery/DeliveryConsentSheet";
import { ActiveDeliveryCard } from "./ActiveDeliveryCard";
import { HomeHeader } from "./HomeHeader";
import { MatchingRequestList } from "./MatchingRequestList";
import { RecentHistoryList } from "./RecentHistoryList";
import { SectionTitle } from "./SectionTitle";

type HomeDashboardProps = {
    role: UserRole;
    content: HomeContent;
    isLoading?: boolean;
    errorMessage?: string;
    onRetry?: () => void;
    avatarUrl?: string | null;
    isNicknameLoading?: boolean;
    isRoleChanging?: boolean;
    onRoleChange: (role: UserRole) => void;
};

export function HomeDashboard({
    role,
    content,
    isLoading = false,
    errorMessage,
    onRetry,
    avatarUrl,
    isNicknameLoading = false,
    isRoleChanging = false,
    onRoleChange,
}: HomeDashboardProps) {
    const navigate = useNavigate();
    const [isConsentOpen, setIsConsentOpen] = useState(false);
    const [isAlarmOpen, setIsAlarmOpen] = useState(false);
    const [isAlarmLoading, setIsAlarmLoading] = useState(false);
    const [alarmError, setAlarmError] = useState("");
    const [alarms, setAlarms] = useState<NotificationItem[]>([]);
    const [unreadCount, setUnreadCount] = useState(0);
    const alarmContainerRef = useRef<HTMLDivElement>(null);

    const loadNotifications = useCallback(async () => {
        setIsAlarmLoading(true);
        setAlarmError("");

        try {
            const page = await notificationApi.getNotifications({
                page: 0,
                size: 20,
            });
            setAlarms(page.content);
        } catch {
            setAlarmError("알림을 불러오지 못했습니다.");
        } finally {
            setIsAlarmLoading(false);
        }
    }, []);

    useEffect(() => {
        void notificationApi
            .getUnreadCount()
            .then((result) => setUnreadCount(result.unreadCount))
            .catch(() => undefined);
    }, []);

    useEffect(() => {
        if (!isAlarmOpen) {
            return;
        }

        const handlePointerDown = (event: MouseEvent) => {
            if (
                alarmContainerRef.current &&
                !alarmContainerRef.current.contains(event.target as Node)
            ) {
                setIsAlarmOpen(false);
            }
        };
        const handleKeyDown = (event: KeyboardEvent) => {
            if (event.key === "Escape") {
                setIsAlarmOpen(false);
            }
        };

        document.addEventListener("mousedown", handlePointerDown);
        document.addEventListener("keydown", handleKeyDown);

        return () => {
            document.removeEventListener("mousedown", handlePointerDown);
            document.removeEventListener("keydown", handleKeyDown);
        };
    }, [isAlarmOpen]);

    const handleAlarmToggle = () => {
        if (isAlarmOpen) {
            setIsAlarmOpen(false);
            return;
        }

        setIsAlarmOpen(true);
        void loadNotifications();
    };

    const handleAlarmSelect = async (alarm: NotificationItem) => {
        if (!alarm.read) {
            try {
                const updatedAlarm = await notificationApi.markAsRead(
                    alarm.notificationId,
                );
                setAlarms((current) =>
                    current.map((item) =>
                        item.notificationId === alarm.notificationId
                            ? updatedAlarm
                            : item,
                    ),
                );
                setUnreadCount((current) => Math.max(0, current - 1));
            } catch {
                setAlarmError("알림을 확인 처리하지 못했습니다.");
            }
        }

        if (
            alarm.type !== "DELIVERY" ||
            alarm.resourceType !== "DELIVERY" ||
            alarm.resourceId === null
        ) {
            return;
        }

        setIsAlarmOpen(false);
        navigate(
            role === "sender"
                ? `/delivery/status/${alarm.resourceId}`
                : `/delivery/tracking/${alarm.resourceId}`,
        );
    };

    const handleClearAllAlarms = async () => {
        if (alarms.length === 0) {
            return;
        }

        setIsAlarmLoading(true);
        setAlarmError("");

        try {
            await Promise.all(
                alarms.map((alarm) =>
                    notificationApi.deleteNotification(alarm.notificationId),
                ),
            );
            setAlarms([]);
            setUnreadCount(0);
        } catch {
            setAlarmError("알림을 모두 삭제하지 못했습니다.");
            await loadNotifications();
        } finally {
            setIsAlarmLoading(false);
        }
    };

    return (
        <section className="page-container flex h-full min-h-0 flex-col overflow-hidden pt-5">
            <div
                className={`flex flex-col min-h-0 flex-1 transition duration-200  ${
                    isConsentOpen ? "pointer-events-none blur-sm" : ""
                }`}
                aria-hidden={isConsentOpen}
            >
                <div className="shrink-0">
                    <HomeHeader
                        name={content.name}
                        headline={content.headline}
                        role={role}
                        avatarUrl={avatarUrl}
                        isNicknameLoading={isNicknameLoading}
                        unreadCount={unreadCount}
                        isNotificationOpen={isAlarmOpen}
                        onNotificationToggle={handleAlarmToggle}
                        notificationContainerRef={alarmContainerRef}
                        notificationPanel={
                            <AlarmPopup
                                alarms={alarms}
                                onClearAll={() => void handleClearAllAlarms()}
                                onSelect={(alarm) =>
                                    void handleAlarmSelect(alarm)
                                }
                                isLoading={isAlarmLoading}
                                errorMessage={alarmError}
                                className="absolute right-0 top-full z-50"
                            />
                        }
                        isRoleChanging={isRoleChanging}
                        onRoleChange={onRoleChange}
                    />
                </div>

                <div className="scrollbar-hidden flex-1 min-h-0 overflow-y-auto mt-6 overscroll-contain pb-16">
                    <section className="mt-6">
                        <SectionTitle accent>진행중인 전달</SectionTitle>
                        {isLoading ? (
                            <div
                                className="mt-3 h-[88px] animate-pulse rounded-lg bg-purple-100"
                                aria-label="전달 목록을 불러오는 중"
                            />
                        ) : errorMessage ? (
                            <div
                                className="mt-3 rounded-lg border border-rose-200 bg-rose-50 px-5 py-4"
                                role="alert"
                            >
                                <p className="text-sm font-medium text-rose-700">
                                    {errorMessage}
                                </p>
                                {onRetry ? (
                                    <button
                                        type="button"
                                        onClick={onRetry}
                                        className="mt-3 text-sm font-bold text-rose-700 underline"
                                    >
                                        다시 시도
                                    </button>
                                ) : null}
                            </div>
                        ) : content.activeDeliveries.length > 0 ? (
                            <div className="mt-3 flex flex-col gap-2.5">
                                {content.activeDeliveries.map((delivery) => (
                                    <ActiveDeliveryCard
                                        key={delivery.id}
                                        delivery={delivery}
                                        role={role}
                                    />
                                ))}
                            </div>
                        ) : (
                            <p className="mt-3 rounded-lg bg-purple-50 px-5 py-5 text-center text-sm font-medium text-purple-500">
                                진행 중인 전달이 없습니다.
                            </p>
                        )}
                    </section>

                    {role === "sender" ? (
                        <section className="mt-10">
                            <SectionTitle>활동 내역</SectionTitle>
                            {isLoading ? (
                                <div className="mt-3.5 flex flex-col gap-2.5 h-[60px]">
                                    {[0, 1].map((item) => (
                                        <div
                                            key={item}
                                            className="h-[66px] animate-pulse rounded-lg bg-gray-50"
                                        />
                                    ))}
                                </div>
                            ) : errorMessage ? null : (
                                <RecentHistoryList
                                    histories={content.recentHistories}
                                />
                            )}
                        </section>
                    ) : (
                        <section className="mt-10">
                            <SectionTitle>매칭 요청</SectionTitle>
                            <MatchingRequestList
                                requests={content.matchingRequests}
                            />
                        </section>
                    )}
                </div>
            </div>
            {content.actionLabel ? (
                <button
                    type="button"
                    onClick={() => setIsConsentOpen(true)}
                    className="fixed bottom-20 left-1/2 z-10 w-[calc(100%-40px)] max-w-[360px] -translate-x-1/2 rounded-lg bg-purple-500 py-3.5 font-bold text-white shadow-sm transition-colors hover:bg-purple-600"
                >
                    {content.actionLabel}
                </button>
            ) : null}

            {isConsentOpen ? (
                <DeliveryConsentSheet
                    onClose={() => setIsConsentOpen(false)}
                    onConfirm={() => {
                        setIsConsentOpen(false);
                        navigate("/delivery/request");
                    }}
                />
            ) : null}
        </section>
    );
}
