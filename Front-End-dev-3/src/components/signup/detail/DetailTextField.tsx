import ValidationMessage from "../common/ValidationMessage";
import { SIGNUP_LABEL_CLASS } from "../common/styles";
import { DETAIL_FIELD_CLASS } from "./constants";

type DetailTextFieldProps = {
  id: string;
  label: string;
  type: "text" | "tel";
  placeholder: string;
  value: string;
  validationMessage: string;
  validationFallback: string;
  showValidation: boolean;
  onChange: (value: string) => void;
};

export default function DetailTextField({
  id,
  label,
  type,
  placeholder,
  value,
  validationMessage,
  validationFallback,
  showValidation,
  onChange,
}: DetailTextFieldProps) {
  return (
    <section className="flex flex-col gap-[4px]">
      <label className={SIGNUP_LABEL_CLASS} htmlFor={id}>
        {label}
      </label>
      <input
        id={id}
        type={type}
        placeholder={placeholder}
        value={value}
        onChange={(event) => onChange(event.target.value)}
        className={DETAIL_FIELD_CLASS}
      />
      <ValidationMessage
        message={validationMessage}
        fallback={validationFallback}
        visible={showValidation && Boolean(validationMessage)}
      />
    </section>
  );
}

