export type ReportTargetType = "DELIVERY" | "CHAT_MESSAGE" | "ACCOUNT";

export const REPORT_TYPE = {
    SPAM: {
        label: "스팸",
        code: "SPAM",
    },
    ABUSE: {
        label: "욕설 및 비방",
        code: "ABUSE",
    },
    FRAUD: {
        label: "사기 및 허위 정보",
        code: "FRAUD",
    },
    HARASSMENT: {
        label: "괴롭힘",
        code: "HARASSMENT",
    },
    INAPPROPRIATE_CONTENT: {
        label: "부적절한 내용",
        code: "INAPPROPRIATE_CONTENT",
    },
    OTHER: {
        label: "기타",
        code: "OTHER",
    },
} as const;

export type ReportTypeKey = keyof typeof REPORT_TYPE;

export type ReportTypeLabel = (typeof REPORT_TYPE)[ReportTypeKey]["label"];
export type ReportTypeFilter = Exclude<
    (typeof REPORT_TYPE)[ReportTypeKey]["code"],
    null
>;

export type Report = {
    targetType: ReportTargetType;
    reason: ReportTypeKey;
    detail: string;
    imageKeys?: string[];

    deliveryId?: number;
    chatMessageId?: number;
    reportedAccountId?: number;
};
