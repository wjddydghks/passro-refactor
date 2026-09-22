import { useNavigate } from "react-router-dom";
import type { ActiveDelivery } from "../../types/home";
import { UserRole } from "../../types/user";

type ActiveDeliveryCardProps = {
    delivery: ActiveDelivery;
    role: UserRole;
};

export function ActiveDeliveryCard({
    delivery,
    role,
}: ActiveDeliveryCardProps) {
    const navigate = useNavigate();
    const handleButton = () => {
        if (role == "sender") {
            navigate(`/delivery/status/${delivery.id}`);
            return;
        } else {
            navigate(`/delivery/tracking/${delivery.id}`);
        }
    };

    return (
        <button
            type="button"
            onClick={handleButton}
            className="grid min-h-[88px] w-full grid-cols-[minmax(0,1fr)_max-content] items-center gap-3 rounded-lg bg-purple-100 px-5 py-4 text-left transition-colors hover:bg-purple-200"
            aria-label={`${delivery.title} 전달 추적 보기`}
        >
            <div className="min-w-0">
                <h3 className="truncate font-bold text-black">
                    {delivery.title}
                </h3>
                <p className="mt-1 line-clamp-2 break-keep text-sm font-semibold leading-5 text-purple-400">
                    {delivery.route}
                </p>
            </div>
            <span
                className="inline-flex items-center justify-center rounded-lg bg-purple-600 px-3 py-2 text-xs font-bold text-white shadow-sm"
                style={{ whiteSpace: "nowrap", wordBreak: "keep-all" }}
            >
                {delivery.status}
            </span>
        </button>
    );
}
