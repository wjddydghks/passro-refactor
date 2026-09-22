import { POINT_FILTER, PointFilterLabel } from "../../types/point";

interface PointFilterButtonProps {
    selected: PointFilterLabel;
    onSelect: (label: PointFilterLabel) => void;
}

export const PointFilterButton = ({
    selected,
    onSelect,
}: PointFilterButtonProps) => {
    const labels: PointFilterLabel[] = Object.values(POINT_FILTER).map(
        (c) => c.label,
    );

    return (
        <div className="flex gap-2 mt-8">
            {labels.map((label) => (
                <button
                    key={label}
                    onClick={() => onSelect(label)}
                    className={`flex items-center px-3 py-2 rounded-md text-xs font-semibold ${selected === label ? "bg-gray-900 text-white" : "border border-gray-200 text-gray-800"}`}
                >
                    {label}
                </button>
            ))}
        </div>
    );
};
