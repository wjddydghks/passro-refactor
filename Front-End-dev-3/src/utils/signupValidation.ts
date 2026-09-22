import type {
  SignupDetailValidationMessages,
  SignupFormData,
  SignupNicknameCheckStatus,
} from "../types/signup";

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
export function isEmailValid(email: string) {
  return EMAIL_PATTERN.test(email.trim());
}

export function getEmailValidationMessage(
  email: string,
  isEmailRequested: boolean,
) {
  const trimmedEmail = email.trim();

  if (!trimmedEmail) {
    return "이메일을 입력해주세요";
  }

  if (!isEmailValid(trimmedEmail)) {
    return "올바른 이메일 형식으로 입력해주세요";
  }

  return isEmailRequested ? "" : "인증 요청을 진행해주세요";
}

export function getPasswordValidation(password: string) {
  const categoryCount = [
    /[A-Z]/.test(password),
    /[a-z]/.test(password),
    /\d/.test(password),
    /[^A-Za-z0-9]/.test(password),
  ].filter(Boolean).length;
  const hasValidLength = password.length >= 6 && password.length <= 20;

  return hasValidLength && categoryCount >= 2;
}

export function getNicknameValidation(
  nickname: string,
  status: SignupNicknameCheckStatus,
) {
  if (!nickname.trim()) {
    return { message: "닉네임을 입력해주세요", color: "text-[#E5484D]" };
  }

  if (status === "available") {
    return { message: "사용 가능한 닉네임입니다", color: "text-[#24A148]" };
  }

  if (status === "duplicate") {
    return { message: "같은 닉네임이 존재합니다", color: "text-[#E5484D]" };
  }

  return {
    message: "닉네임 중복 확인을 진행해주세요",
    color: "text-[#E5484D]",
  };
}

export function getDetailValidationMessages(
  formData: SignupFormData,
): SignupDetailValidationMessages {
  const isPhoneValid = /^\d{2,3}-\d{3,4}-\d{4}$/.test(formData.phone);

  return {
    name: formData.name.trim() ? "" : "이름을 입력해주세요",
    phone: !formData.phone
      ? "전화번호를 입력해주세요"
      : isPhoneValid
        ? ""
        : "전화번호 형식을 확인해주세요",
    birthDate: formData.birthDate ? "" : "생년월일을 선택해주세요",
    originStation: formData.originStationId > 0 ? "" : "출발역을 선택해주세요",
    destinationStation: formData.destinationStationId > 0
      ? ""
      : "도착역을 선택해주세요",
  };
}

export function hasDetailValidationError(
  messages: SignupDetailValidationMessages,
) {
  return Object.values(messages).some(Boolean);
}

