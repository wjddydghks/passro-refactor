import { useState, type FormEvent } from "react";
import type {
  SignupEmailVerificationStatus,
  SignupFieldUpdater,
  SignupFormData,
  SignupNicknameCheckStatus,
} from "../../../types/signup";
import {
  getEmailValidationMessage,
  getPasswordValidation,
  isEmailValid,
} from "../../../utils/signupValidation";
import { authApi } from "../../../apis/authApi";
import { ApiError } from "../../../types/api";
import FeedbackModal from "../common/FeedbackModal";
import SignupSubmitButton from "../common/SignupSubmitButton";
import EmailField from "./EmailField";
import NicknameField from "./NicknameField";
import PasswordFields from "./PasswordFields";

type BasicSignupFormProps = {
  formData: SignupFormData;
  updateField: SignupFieldUpdater;
  onSubmit: (event: FormEvent<HTMLFormElement>) => void;
  emailVerificationStatus: SignupEmailVerificationStatus;
  onEmailVerificationStatusChange: (
    status: SignupEmailVerificationStatus,
  ) => void;
};

export default function BasicSignupForm({
  formData,
  updateField,
  onSubmit,
  emailVerificationStatus,
  onEmailVerificationStatusChange,
}: BasicSignupFormProps) {
  const [showValidation, setShowValidation] = useState(false);
  const [emailAction, setEmailAction] = useState<
    "check" | "send" | "confirm" | null
  >(null);
  const [emailAvailabilityStatus, setEmailAvailabilityStatus] = useState<
    "idle" | "available" | "duplicate"
  >(emailVerificationStatus === "idle" ? "idle" : "available");
  const [emailError, setEmailError] = useState("");
  const [nicknameCheckStatus, setNicknameCheckStatus] =
    useState<SignupNicknameCheckStatus>("idle");
  const [modalMessage, setModalMessage] = useState("");

  const handleEmailChange = (value: string) => {
    updateField("email", value);
    updateField("emailCode", "");
    setEmailAvailabilityStatus("idle");
    setEmailError("");
    onEmailVerificationStatusChange("idle");
  };

  const handleEmailRequest = async () => {
    if (!isEmailValid(formData.email)) {
      setEmailAvailabilityStatus("idle");
      setEmailError(
        getEmailValidationMessage(formData.email, true),
      );
      return;
    }

    const mail = formData.email.trim();
    let requestPhase: "check" | "send" = "check";

    setEmailAction("check");
    setEmailError("");
    setModalMessage("");

    try {
      const available = await authApi.checkEmailAvailable(mail);

      if (!available) {
        setEmailAvailabilityStatus("duplicate");
        setEmailError("이미 사용 중인 이메일입니다.");
        updateField("emailCode", "");
        onEmailVerificationStatusChange("idle");
        return;
      }

      setEmailAvailabilityStatus("available");
      requestPhase = "send";
      setEmailAction("send");

      await authApi.sendMail({
        mail,
        student: false,
      });
      onEmailVerificationStatusChange("sent");
      setModalMessage("인증번호를 이메일로 발송했습니다.");
    } catch (error) {
      if (error instanceof ApiError && error.code === "ACCOUNT400_4") {
        setEmailAvailabilityStatus("duplicate");
        setEmailError("이미 사용 중인 이메일입니다.");
        updateField("emailCode", "");
        onEmailVerificationStatusChange("idle");
      } else if (
        !(error instanceof ApiError) ||
        error.status === undefined ||
        error.status >= 500
      ) {
        setModalMessage(
          "서버에 연결할 수 없습니다. 잠시 후 다시 시도해주세요.",
        );
      } else {
        setModalMessage(
          requestPhase === "check"
            ? "이메일 중복 확인에 실패했습니다. 다시 시도해주세요."
            : "인증번호를 전송하지 못했습니다. 다시 시도해주세요.",
        );
      }
    } finally {
      setEmailAction(null);
    }
  };

  const handleEmailConfirm = async () => {
    if (!/^\d{6}$/.test(formData.emailCode)) {
      setModalMessage("6자리 인증번호를 입력해주세요.");
      return;
    }

    setEmailAction("confirm");
    setModalMessage("");

    try {
      await authApi.confirmMail({
        mail: formData.email.trim(),
        code: formData.emailCode,
      });
      onEmailVerificationStatusChange("verified");
      setModalMessage("이메일 인증이 완료되었습니다.");
    } catch (error) {
      setModalMessage(
        error instanceof ApiError
          ? error.message
          : "인증번호 확인 중 오류가 발생했습니다.",
      );
    } finally {
      setEmailAction(null);
    }
  };

  const handleNicknameChange = (value: string) => {
    updateField("nickname", value);
    setNicknameCheckStatus("idle");
  };

  const handleNicknameCheck = async () => {
    const nickname = formData.nickname.trim();

    if (!nickname) {
      setShowValidation(true);
      setNicknameCheckStatus("idle");
      setModalMessage("닉네임을 입력해주세요.");
      return;
    }

    const available = await authApi.checkNicknameAvailable(nickname);

    if (!available) {
      setShowValidation(true);
      setNicknameCheckStatus("duplicate");
      return;
    }

    setNicknameCheckStatus("available");
    // setModalMessage(
    //   "최종 회원가입 단계에서 닉네임 중복 여부를 확인합니다.",
    // );
  };

  const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setShowValidation(true);

    const isFormValid =
      !getEmailValidationMessage(
        formData.email,
        emailVerificationStatus === "verified",
      ) &&
      emailAvailabilityStatus === "available" &&
      getPasswordValidation(formData.password) &&
      formData.passwordCheck.length > 0 &&
      formData.password === formData.passwordCheck &&
      nicknameCheckStatus === "available";

    if (!isFormValid) {
      return;
    }

    onSubmit(event);
  };

  return (
    <form
      onSubmit={handleSubmit}
      noValidate
      className="flex min-h-0 flex-1 flex-col"
    >
      <div className="scrollbar-hidden flex flex-1 flex-col gap-[27px] overflow-y-auto pb-6 pt-[38px]">
        <EmailField
          value={formData.email}
          code={formData.emailCode}
          status={emailVerificationStatus}
          errorMessage={emailError}
          isChecking={emailAction === "check"}
          isSending={emailAction === "send"}
          isConfirming={emailAction === "confirm"}
          showValidation={showValidation}
          onChange={handleEmailChange}
          onCodeChange={(value) => updateField("emailCode", value)}
          onRequest={handleEmailRequest}
          onConfirm={handleEmailConfirm}
        />
        <PasswordFields
          password={formData.password}
          passwordCheck={formData.passwordCheck}
          showValidation={showValidation}
          onPasswordChange={(value) => updateField("password", value)}
          onPasswordCheckChange={(value) => updateField("passwordCheck", value)}
        />
        <NicknameField
          value={formData.nickname}
          status={nicknameCheckStatus}
          showValidation={showValidation}
          onChange={handleNicknameChange}
          onCheck={handleNicknameCheck}
        />
      </div>

      <div className="shrink-0 pt-4 [&>button]:mt-0">
        <SignupSubmitButton>다음</SignupSubmitButton>
      </div>
      <FeedbackModal
        message={modalMessage}
        onClose={() => setModalMessage("")}
      />
    </form>
  );
}
