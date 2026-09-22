import { useEffect, useMemo, useState } from "react";
import type { SignupDateValue } from "../../../types/signup";
import {
    formatSignupDate,
    getDaysInMonth,
    parseSignupDate,
} from "../../../utils/signupDate";
import WheelColumn from "./WheelColumn";

type DatePickerSheetProps = {
    value: string;
    onClose: () => void;
    onConfirm: (value: string) => void;
};

export default function DatePickerSheet({
    value,
    onClose,
    onConfirm,
}: DatePickerSheetProps) {
    const currentYear = new Date().getFullYear();
    const [selectedDate, setSelectedDate] = useState<SignupDateValue>(() =>
        parseSignupDate(value),
    );
    const years = useMemo(
        () =>
            Array.from(
                { length: currentYear - 1920 + 1 },
                (_, index) => 1920 + index,
            ),
        [currentYear],
    );
    const months = useMemo(
        () => Array.from({ length: 12 }, (_, index) => index + 1),
        [],
    );
    const days = useMemo(
        () =>
            Array.from(
                {
                    length: getDaysInMonth(
                        selectedDate.year,
                        selectedDate.month,
                    ),
                },
                (_, index) => index + 1,
            ),
        [selectedDate.month, selectedDate.year],
    );

    useEffect(() => {
        const lastDay = getDaysInMonth(selectedDate.year, selectedDate.month);

        if (selectedDate.day > lastDay) {
            setSelectedDate((previous) => ({ ...previous, day: lastDay }));
        }
    }, [selectedDate.day, selectedDate.month, selectedDate.year]);

    const updateSelectedDate = (nextValue: Partial<SignupDateValue>) => {
        setSelectedDate((previous) => ({ ...previous, ...nextValue }));
    };

    return (
        <div
            className="bottom-sheet-backdrop-in fixed inset-0 z-30 mx-auto flex w-full max-w-[402px] items-end overflow-hidden bg-black/40 pb-16"
            onClick={onClose}
        >
            <div
                className="bottom-sheet-panel-in relative h-[460px] w-full rounded-t-[30px] bg-white px-5 pb-2.5 pt-[17px]"
                role="dialog"
                aria-modal="true"
                aria-labelledby="date-picker-title"
                onClick={(event) => event.stopPropagation()}
            >
                <div className="mx-auto h-1 w-[55px] rounded-full bg-gray-200" />
                <h2
                    id="date-picker-title"
                    className="mt-6 text-center text-2xl font-semibold text-gray-700"
                >
                    날짜 선택
                </h2>

                <div className="relative mt-8 h-[218px]">
                    <div className="pointer-events-none absolute left-0 right-0 top-1/2 -translate-y-1 z-0 h-[42px] rounded-lg bg-gray-50" />
                    <div className="relative z-10 grid h-full grid-cols-3">
                        <WheelColumn
                            options={years}
                            selectedValue={selectedDate.year}
                            suffix="년"
                            onChange={(year) => updateSelectedDate({ year })}
                        />
                        <WheelColumn
                            options={months}
                            selectedValue={selectedDate.month}
                            suffix="월"
                            onChange={(month) => updateSelectedDate({ month })}
                        />
                        <WheelColumn
                            options={days}
                            selectedValue={selectedDate.day}
                            suffix="일"
                            onChange={(day) => updateSelectedDate({ day })}
                        />
                    </div>
                </div>

                <button
                    type="button"
                    onClick={() => onConfirm(formatSignupDate(selectedDate))}
                    className="absolute bottom-10 left-5 right-5 flex py-3.5 items-center justify-center rounded-lg bg-gray-800 font-bold text-white transition-colors hover:bg-gray-900"
                >
                    완료
                </button>
            </div>
        </div>
    );
}
