import type { ReactNode, Ref } from "react";
import type { UserRole } from "../../types/user";
import { RoleAvatar } from "./RoleAvatar";

type HomeHeaderProps = {
    name: string;
    headline: string;
    role: UserRole;
    avatarUrl?: string | null;
    isNicknameLoading?: boolean;
    unreadCount?: number;
    isNotificationOpen?: boolean;
    onNotificationToggle?: () => void;
    notificationPanel?: ReactNode;
    notificationContainerRef?: Ref<HTMLDivElement>;
    isRoleChanging?: boolean;
    onRoleChange: (role: UserRole) => void;
};

export function HomeHeader({
    name,
    headline,
    role,
    avatarUrl,
    isNicknameLoading = false,
    unreadCount = 0,
    isNotificationOpen = false,
    onNotificationToggle,
    notificationPanel,
    notificationContainerRef,
    isRoleChanging = false,
    onRoleChange,
}: HomeHeaderProps) {
    return (
        <div>
            <div className="flex items-center justify-between gap-4">
                <p
                    className="text-[21px] font-bold leading-[30px] tracking-normal text-gray-900"
                    aria-live="polite"
                >
                    안녕하세요,{" "}
                    {isNicknameLoading ? (
                        <span
                            className="inline-block h-[21px] w-20 animate-pulse rounded bg-gray-200 align-[-2px]"
                            aria-label="닉네임을 불러오는 중"
                        />
                    ) : (
                        name
                    )}
                    님!
                    <br />
                    {headline}
                </p>
                <div ref={notificationContainerRef} className="relative shrink-0">
                    <button
                        type="button"
                        onClick={onNotificationToggle}
                        className="relative block h-[60px] w-[60px] rounded-full focus:outline-none focus-visible:ring-2 focus-visible:ring-purple-500 focus-visible:ring-offset-2"
                        aria-label="알림 열기"
                        aria-haspopup="dialog"
                        aria-expanded={isNotificationOpen}
                    >
                        <span className="block h-[60px] w-[60px] overflow-hidden rounded-full border border-gray-200 bg-white">
                            <img
                                src={avatarUrl || "/Logo.png"}
                                alt=""
                                className={`h-full w-full ${avatarUrl
                                        ? "object-cover"
                                        : "object-contain p-1"
                                    }`}
                            />
                        </span>
                        {unreadCount > 0 ? (
                            <span className="absolute -right-[3px] -top-[3px] flex h-[23px] min-w-[23px] items-center justify-center rounded-full bg-errorRed px-1 text-center text-sm font-medium leading-none text-white">
                                {unreadCount > 99 ? "99+" : unreadCount}
                            </span>
                        ) : null}
                    </button>
                    <div className="h-2"></div>
                    {isNotificationOpen ? notificationPanel : null}
                </div>
            </div>
            <div className="mt-12">
                <RoleAvatar
                    role={role}
                    disabled={isRoleChanging}
                    onRoleChange={onRoleChange}
                />
            </div>
        </div>
    );
}
