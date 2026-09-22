interface ChatAvatarProps {
    name: string;
    picture?: string | null;
    className?: string;
}

export default function ChatAvatar({
    name,
    picture,
    className = "h-12 w-12",
}: ChatAvatarProps) {
    return (
        <span
            className={`relative flex shrink-0 items-center justify-center overflow-hidden rounded-full bg-purple-100 font-bold text-purple-600 ${className}`}
            aria-hidden="true"
        >
            {name.trim().charAt(0) || "?"}
            {picture ? (
                <img
                    src={picture}
                    alt=""
                    className="absolute inset-0 h-full w-full object-cover"
                    onError={(event) => {
                        event.currentTarget.hidden = true;
                    }}
                />
            ) : null}
        </span>
    );
}
