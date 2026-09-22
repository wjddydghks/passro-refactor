import type { UserRole } from "../../types/user";

type RoleAvatarProps = {
    role: UserRole;
    disabled?: boolean;
    onRoleChange: (role: UserRole) => void;
};

export function RoleAvatar({
    role,
    disabled = false,
    onRoleChange,
}: RoleAvatarProps) {
    const options: Array<{ label: string; value: UserRole }> = [
        { label: "전달", value: "shipper" },
        { label: "발송", value: "sender" },
    ];

    return (
        <div
            role="radiogroup"
            aria-label="홈 모드 선택"
            className="mx-auto grid h-[50px] w-full max-w-[360px] grid-cols-2 rounded-[9px] bg-gray-100 p-[5px]"
        >
            {options.map((option) => {
                const isSelected = role === option.value;

                return (
                    <button
                        key={option.value}
                        type="button"
                        role="radio"
                        aria-checked={isSelected}
                        disabled={disabled}
                        onClick={() => onRoleChange(option.value)}
                        className={`flex h-10 items-center justify-center rounded-[7px] text-sm leading-[22px] transition-[background-color,color,box-shadow] duration-200 focus:outline-none focus-visible:ring-2 focus-visible:ring-purple-500 focus-visible:ring-inset disabled:cursor-wait disabled:opacity-60 motion-reduce:transition-none ${
                            isSelected
                                ? "bg-white font-semibold text-gray-900 shadow-[0_0_5px_rgba(29,30,35,0.15)]"
                                : "font-medium text-gray-500"
                        }`}
                    >
                        {option.label}
                    </button>
                );
            })}
        </div>
    );
}
