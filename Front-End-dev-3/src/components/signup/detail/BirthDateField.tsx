import CalendarIcon from "../../../assets/icons/CalendarIcon";
import { formatBirthDate } from "../../../utils/signupFormatters";
import ValidationMessage from "../common/ValidationMessage";
import { SIGNUP_LABEL_CLASS } from "../common/styles";
import { DETAIL_FIELD_CLASS } from "./constants";

type BirthDateFieldProps = {
    value: string;
    validationMessage: string;
    showValidation: boolean;
    onOpen: () => void;
};

export default function BirthDateField({
    value,
    validationMessage,
    showValidation,
    onOpen,
}: BirthDateFieldProps) {
    return (
        <section className="flex flex-col gap-[4px]">
            <span className={SIGNUP_LABEL_CLASS}>생년월일</span>
            <button
                type="button"
                onClick={onOpen}
                className={`${DETAIL_FIELD_CLASS} flex items-center justify-between leading-none`}
                aria-label="생년월일 선택"
            >
                <span
                    className={`flex h-full items-center ${
                        value ? "text-gray-800" : "text-gray-500"
                    }`}
                >
                    {value ? formatBirthDate(value) : "생년월일을 입력해주세요"}
                </span>
                <span className="text-gray-400">
                    <CalendarIcon />
                </span>
            </button>
            <ValidationMessage
                message={validationMessage}
                fallback="생년월일 검증 메시지"
                visible={showValidation && Boolean(validationMessage)}
            />
        </section>
    );
}
