import ValidationMessage from "../common/ValidationMessage";
import { SIGNUP_LABEL_CLASS } from "../common/styles";
import { getPasswordValidation } from "../../../utils/signupValidation";
import { BASIC_FIELD_CLASS } from "./constants";

type PasswordFieldsProps = {
  password: string;
  passwordCheck: string;
  showValidation: boolean;
  onPasswordChange: (value: string) => void;
  onPasswordCheckChange: (value: string) => void;
};

export default function PasswordFields({
  password,
  passwordCheck,
  showValidation,
  onPasswordChange,
  onPasswordCheckChange,
}: PasswordFieldsProps) {
  const isPasswordValid = getPasswordValidation(password);
  const isPasswordCheckValid =
    passwordCheck.length > 0 && password === passwordCheck;

  return (
    <>
      <section className="flex flex-col gap-[10px]">
        <label className={SIGNUP_LABEL_CLASS} htmlFor="signup-password">
          비밀번호
        </label>
        <input
          id="signup-password"
          type="password"
          placeholder="비밀번호를 입력해주세요"
          value={password}
          onChange={(event) => onPasswordChange(event.target.value)}
          className={BASIC_FIELD_CLASS}
        />
        <p
          className={`pl-1 text-[11px] font-medium leading-[18px] min-[390px]:whitespace-nowrap min-[390px]:text-[12px] ${isPasswordValid
              ? "text-[#24A148]"
              : showValidation
                ? "text-[#E5484D]"
                : "text-gray-500"
            }`}
          aria-live="polite"
        >
          6~20자 영문 대문자, 소문자, 숫자, 특수문자 중 2가지 이상 조합
        </p>
      </section>

      <section className="flex flex-col gap-[10px]">
        <label className={SIGNUP_LABEL_CLASS} htmlFor="signup-password-check">
          비밀번호 확인
        </label>
        <input
          id="signup-password-check"
          type="password"
          placeholder="비밀번호를 확인해주세요"
          value={passwordCheck}
          onChange={(event) => onPasswordCheckChange(event.target.value)}
          className={BASIC_FIELD_CLASS}
        />
        <ValidationMessage
          message={
            isPasswordCheckValid
              ? "비밀번호가 일치합니다"
              : "비밀번호가 일치하지 않습니다."
          }
          fallback=""
          colorClass={
            isPasswordCheckValid
              ? "text-[#24A148]"
              : "text-[#E5484D]"
          }
          visible={isPasswordCheckValid || showValidation}
        />
      </section>
    </>
  );
}
