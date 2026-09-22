import {
    DELIVERY_FILTER,
    DeliveryFilterLabel,
} from "../../../types/delivery/delivery";

interface DeliveryFilterButtonProps {
    selected: DeliveryFilterLabel;
    onSelect: (label: DeliveryFilterLabel) => void;
}

export const DeliveryFilterButton = ({
    selected,
    onSelect,
}: DeliveryFilterButtonProps) => {
    const labels: DeliveryFilterLabel[] = Object.values(DELIVERY_FILTER).map(
        (c) => c.label,
    );

    return (
        <div className="flex gap-2">
            {labels.map((label) => (
                <button
                    key={label}
                    onClick={() => onSelect(label)}
                    className={`flex px-3 py-2 rounded-md text-xs font-semibold ${selected === label ? "bg-gray-900 text-white" : "border border-gray-200 text-gray-800 "}`}
                >
                    {label}
                </button>
            ))}
        </div>
    );
};
