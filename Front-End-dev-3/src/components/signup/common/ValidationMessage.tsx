import { SIGNUP_VALIDATION_CLASS } from "./styles";

type ValidationMessageProps = {
  message: string;
  fallback: string;
  visible?: boolean;
  colorClass?: string;
  className?: string;
};

export default function ValidationMessage({
  message,
  fallback,
  visible = true,
  colorClass = "text-[#E5484D]",
  className = "",
}: ValidationMessageProps) {
  return (
    <p
      className={`${SIGNUP_VALIDATION_CLASS} ${colorClass} ${
        visible ? "" : "invisible"
      } ${className}`}
      aria-live="polite"
    >
      {message || fallback}
    </p>
  );
}

