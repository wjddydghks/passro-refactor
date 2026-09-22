import PageHeader from "../common/PageHeader";

function SkeletonBlock({ className }: { className: string }) {
    return (
        <div
            className={`animate-pulse rounded bg-gray-200 ${className}`}
            aria-hidden="true"
        />
    );
}

export default function ChatRoomSkeleton() {
    return (
        <div
            className="relative flex h-full min-h-0 w-full flex-col overflow-hidden bg-white"
            aria-busy="true"
            aria-label="채팅방을 불러오는 중"
        >
            <div className="w-full shrink-0 border-b border-gray-100 bg-white px-4 pb-4 pt-3">
                <PageHeader
                    title={<SkeletonBlock className="h-6 w-20" />}
                    className="mb-3"
                    rightAction={<SkeletonBlock className="h-6 w-6 rounded-full" />}
                />

                <div className="flex h-[76px] w-full items-center justify-between rounded-2xl bg-[#F7F7F9] p-4">
                    <div className="flex flex-col gap-2">
                        <SkeletonBlock className="h-[18px] w-28" />
                        <SkeletonBlock className="h-4 w-44" />
                    </div>
                    <SkeletonBlock className="h-[30px] w-14 rounded-xl bg-purple-100" />
                </div>
            </div>

            <div className="min-h-0 flex-1 space-y-4 overflow-hidden px-4 py-6">
                <p
                    className="pb-1 text-center text-sm font-medium text-gray-400"
                    role="status"
                >
                    채팅을 불러오는 중입니다.
                </p>
                <div className="flex justify-start">
                    <SkeletonBlock className="h-12 w-[58%] rounded-3xl" />
                </div>
                <div className="flex justify-end">
                    <SkeletonBlock className="h-12 w-[48%] rounded-3xl bg-purple-100" />
                </div>
                <div className="flex justify-start">
                    <SkeletonBlock className="h-[72px] w-[68%] rounded-3xl" />
                </div>
                <div className="flex justify-end">
                    <SkeletonBlock className="h-12 w-[38%] rounded-3xl bg-purple-100" />
                </div>
            </div>

            <div className="w-full shrink-0 bg-white px-4 py-4">
                <div className="flex h-12 items-center justify-between rounded-[24px] bg-[#F7F7F9] px-4">
                    <SkeletonBlock className="h-4 w-32 bg-gray-200" />
                    <SkeletonBlock className="h-8 w-8 rounded-full" />
                </div>
            </div>

        </div>
    );
}
