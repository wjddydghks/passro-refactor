const KOREA_TIME_ZONE = "Asia/Seoul";
const TIME_ZONE_SUFFIX_PATTERN = /(?:Z|[+-]\d{2}:?\d{2})$/u;

type CalendarDate = {
    year: number;
    month: number;
    day: number;
};

export function parseChatDateTime(value: string) {
    const normalizedValue = TIME_ZONE_SUFFIX_PATTERN.test(value)
        ? value
        : `${value}`;

    return new Date(normalizedValue);
}

export function formatChatTime(value: string) {
    const date = parseChatDateTime(value);

    if (Number.isNaN(date.getTime())) {
        return "";
    }

    return new Intl.DateTimeFormat("ko-KR", {
        hour: "numeric",
        minute: "2-digit",
        hour12: true,
        timeZone: KOREA_TIME_ZONE,
    }).format(date);
}

export function formatChatRoomDateTime(value: string | null) {
    if (!value) {
        return "";
    }

    const date = parseChatDateTime(value);
    if (Number.isNaN(date.getTime())) {
        return "";
    }

    const dateParts = getKoreanCalendarDate(date);
    const todayParts = getKoreanCalendarDate(new Date());

    if (isSameCalendarDate(dateParts, todayParts)) {
        return formatChatTime(value);
    }

    if (isPreviousCalendarDate(dateParts, todayParts)) {
        return "어제";
    }

    if (dateParts.year === todayParts.year) {
        return `${dateParts.month}월 ${dateParts.day}일`;
    }

    return `${dateParts.year}.${String(dateParts.month).padStart(2, "0")}.${String(dateParts.day).padStart(2, "0")}`;
}

function getKoreanCalendarDate(date: Date): CalendarDate {
    const parts = new Intl.DateTimeFormat("en-CA", {
        year: "numeric",
        month: "numeric",
        day: "numeric",
        timeZone: KOREA_TIME_ZONE,
    }).formatToParts(date);
    const getPart = (type: Intl.DateTimeFormatPartTypes) =>
        Number(parts.find((part) => part.type === type)?.value);

    return {
        year: getPart("year"),
        month: getPart("month"),
        day: getPart("day"),
    };
}

function isSameCalendarDate(left: CalendarDate, right: CalendarDate) {
    return (
        left.year === right.year &&
        left.month === right.month &&
        left.day === right.day
    );
}

function isPreviousCalendarDate(
    candidate: CalendarDate,
    today: CalendarDate,
) {
    const previousDate = new Date(
        Date.UTC(today.year, today.month - 1, today.day - 1),
    );

    return isSameCalendarDate(candidate, {
        year: previousDate.getUTCFullYear(),
        month: previousDate.getUTCMonth() + 1,
        day: previousDate.getUTCDate(),
    });
}
