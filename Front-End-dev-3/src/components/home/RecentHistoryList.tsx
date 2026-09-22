import { useNavigate } from "react-router-dom";
import type { RecentHistory } from "../../types/home";

type RecentHistoryListProps = {
    histories: RecentHistory[];
};

export function RecentHistoryList({ histories }: RecentHistoryListProps) {
    const navigate = useNavigate();

    if (histories.length === 0) {
        return (
            <p className="mt-3.5 rounded-lg bg-gray-50 px-5 py-5 text-center text-sm font-medium text-gray-500">
                활동 내역이 없습니다.
            </p>
        );
    }

    return (
        <div className="mt-3.5 flex flex-col gap-2.5">
            {histories.map((history) => (
                <button
                    type="button"
                    key={history.id}
                    onClick={() =>
                        navigate(`/delivery/status/${history.id}`)
                    }
                    className="flex items-center justify-between gap-3 rounded-lg bg-gray-50 px-5 py-3 text-left transition-colors hover:bg-gray-100"
                >
                    <div className="min-w-0">
                        <h3 className="truncate text-sm font-bold text-gray-800">
                            {history.title}
                        </h3>
                        <p className="mt-0.5 text-xs font-semibold text-gray-500">
                            {history.route}
                        </p>
                    </div>
                    <span className="rounded-lg bg-gray-300 px-3 py-2 text-xs font-bold text-white">
                        {history.status}
                    </span>
                </button>
            ))}
        </div>
    );
}
