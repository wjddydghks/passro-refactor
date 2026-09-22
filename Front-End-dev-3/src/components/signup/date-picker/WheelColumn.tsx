import { useRef, type PointerEvent, type WheelEvent } from "react";

const VISIBLE_SIDE_COUNT = 3;

type WheelColumnProps = {
  options: number[];
  selectedValue: number;
  suffix: string;
  onChange: (value: number) => void;
};

export default function WheelColumn({
  options,
  selectedValue,
  suffix,
  onChange,
}: WheelColumnProps) {
  const pointerYRef = useRef<number | null>(null);
  const selectedIndex = options.indexOf(selectedValue);
  const visibleOptions = Array.from(
    { length: VISIBLE_SIDE_COUNT * 2 + 1 },
    (_, index) => options[selectedIndex + index - VISIBLE_SIDE_COUNT] ?? null,
  );

  const moveSelection = (direction: -1 | 1) => {
    const nextIndex = selectedIndex + direction;

    if (nextIndex >= 0 && nextIndex < options.length) {
      onChange(options[nextIndex]);
    }
  };

  const handleWheel = (event: WheelEvent<HTMLDivElement>) => {
    event.preventDefault();

    if (Math.abs(event.deltaY) >= 1) {
      moveSelection(event.deltaY > 0 ? 1 : -1);
    }
  };

  const handlePointerMove = (event: PointerEvent<HTMLDivElement>) => {
    if (pointerYRef.current === null) {
      return;
    }

    const diff = event.clientY - pointerYRef.current;

    if (Math.abs(diff) >= 24) {
      moveSelection(diff < 0 ? 1 : -1);
      pointerYRef.current = event.clientY;
    }
  };

  return (
    <div
      onWheel={handleWheel}
      onPointerDown={(event) => {
        pointerYRef.current = event.clientY;
        event.currentTarget.setPointerCapture(event.pointerId);
      }}
      onPointerMove={handlePointerMove}
      onPointerUp={() => {
        pointerYRef.current = null;
      }}
      onPointerCancel={() => {
        pointerYRef.current = null;
      }}
      className="h-full touch-none select-none overflow-hidden px-1"
    >
      {visibleOptions.map((option, index) => {
        if (option === null) {
          return <div key={`empty-${index}`} className="h-9" />;
        }

        const isSelected = option === selectedValue;

        return (
          <button
            key={option}
            type="button"
            onClick={() => onChange(option)}
            className={`flex h-9 w-full snap-center items-center justify-center rounded-[10px] text-[22px] leading-9 ${
              isSelected
                ? "font-semibold text-gray-700"
                : "font-medium text-gray-200"
            }`}
          >
            {option}
            {suffix}
          </button>
        );
      })}
    </div>
  );
}
