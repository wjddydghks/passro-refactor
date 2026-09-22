import { useNavigate } from "react-router-dom";
import ArrowIcon from "../../../assets/icons/ArrowIcon";
import {
    DELIVERY_FILTER,
    DeliveryStatus,
} from "../../../types/delivery/delivery";
import { UserRole } from "../../../types/user";

interface DeliveryItem {
    id: number;
    name: string;
    status: DeliveryStatus;
    start?: string;
    end?: string;
    date?: string;
    role: UserRole;
}

interface DeliveryListProps {
    items: DeliveryItem[];
}

export const DeliveryList = ({ items }: DeliveryListProps) => {
    const navigate = useNavigate();

    const getStatusLabel = (status: DeliveryStatus) => {
        switch (status) {
            case "WAITING_PICKUP":
                return "픽업 대기";
            case "DELIVERING":
                return DELIVERY_FILTER.DELIVERING.label;
            case "COMPLETED":
                return DELIVERY_FILTER.COMPLETED.label;
        }
    };

    const getStatusClassName = (status: DeliveryStatus) => {
        switch (status) {
            case "WAITING_PICKUP":
                return "bg-purple-600";
            case "DELIVERING":
                return "bg-purple-600";
            case "COMPLETED":
                return "bg-gray-300";
        }
    };

    return (
        <div className="scrollbar-hidden min-h-0 flex-1 overflow-y-auto overscroll-contain mt-6 flex w-full flex-col gap-3.5 pb-16">
            {items.map((delivery) => {
                const statusLabel = getStatusLabel(delivery.status);
                return (
                    <button
                        type="button"
                        key={delivery.id}
                        onClick={() => {
                            if (delivery.role === "sender") {
                                navigate(`/delivery/status/${delivery.id}`);
                            } else {
                                navigate(`/delivery/tracking/${delivery.id}`);
                            }
                        }}
                        className="flex items-center justify-between rounded-lg bg-gray-50 px-5 py-4"
                    >
                        <div className="flex flex-col items-start text-left gap-1">
                            <div className="font-bold">{delivery.name}</div>
                            {delivery.status !== "COMPLETED" ? (
                                <div className="flex items-center text-sm font-semibold text-gray-500">
                                    <div>{delivery.start}</div>
                                    <ArrowIcon />
                                    <div>{delivery.end}</div>
                                </div>
                            ) : (
                                <div className="text-sm font-semibold text-gray-500">
                                    {delivery.date}
                                </div>
                            )}
                        </div>

                        <div
                            className={`flex items-center justify-center rounded-lg px-2.5 py-1.5 text-xs font-bold text-white ${getStatusClassName(
                                delivery.status,
                            )}`}
                        >
                            {statusLabel}
                        </div>
                    </button>
                );
            })}
        </div>
    );
};
