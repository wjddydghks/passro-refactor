import { PointFilter } from "../../types/point";

interface PointItem {
    id: number;
    name: string;
    date: string;
    amount: number;
    type: PointFilter;
}

interface PointListProps {
    items: PointItem[];
}

export const PointList = ({ items }: PointListProps) => {
    return (
        <div className="flex flex-col w-full gap-3.5">
            {items.map((item) => {
                const sign = item.type === "SAVING" ? "+" : "-";
                return (
                    <div className="flex justify-between px-5 py-4 rounded-lg bg-gray-50">
                        <div className="flex flex-col gap-1">
                            <div className="font-bold text-gray-800">
                                {item.name}
                            </div>
                            <div className="font-semibold text-sm text-gray-500">
                                {item.date}
                            </div>
                        </div>
                        <div
                            className={`font-bold ${item.type === "SAVING" ? "text-purple-600" : "text-errorRed"}`}
                        >
                            {sign} {item.amount.toLocaleString()}p
                        </div>
                    </div>
                );
            })}
        </div>
    );
};
