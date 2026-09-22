import type { ReactNode } from "react";
import { SIGNUP_PRIMARY_BUTTON_CLASS } from "./styles";

type SignupSubmitButtonProps = {
  children: ReactNode;
  disabled?: boolean;
};

export default function SignupSubmitButton({
  children,
  disabled = false,
}: SignupSubmitButtonProps) {
  return (
    <button
      type="submit"
      disabled={disabled}
      className={`${SIGNUP_PRIMARY_BUTTON_CLASS} disabled:cursor-not-allowed disabled:opacity-60`}
    >
      {children}
    </button>
  );
}

