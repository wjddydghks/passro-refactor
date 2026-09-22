export type ChatMessage = {
    id: number;
    sender: "me" | "other";
    text: string;
};

export type ChatRoom = {
    id: string;
    participantName: string;
    participantPicture?: string | null;
    itemName: string;
    route: string;
    status: string;
    lastMessage: string;
    updatedAt: string;
    unreadCount: number;
    messages: ChatMessage[];
};

export const chatRooms: ChatRoom[] = [
    {
        id: "1",
        participantName: "송현수",
        itemName: "무인양품 티셔츠",
        route: "안양 → 정왕역",
        status: "전달중",
        lastMessage: "5분 뒤에 도착합니다. 감사합니다!",
        updatedAt: "오후 2:31",
        unreadCount: 1,
        messages: [
            { id: 1, sender: "me", text: "픽업 장소 도착했습니다!" },
            {
                id: 2,
                sender: "other",
                text: "5분 뒤에 도착합니다. 감사합니다!",
            },
        ],
    },
    {
        id: "2",
        participantName: "김민지",
        itemName: "프로그래밍 전공책",
        route: "범계 → 금정역",
        status: "매칭완료",
        lastMessage: "역 2번 출구에서 만나요!",
        updatedAt: "오전 11:08",
        unreadCount: 0,
        messages: [
            {
                id: 1,
                sender: "other",
                text: "역 2번 출구에서 만나요!",
            },
        ],
    },
    {
        id: "3",
        participantName: "이서준",
        itemName: "오렌지 한 박스",
        route: "안양 → 인덕원역",
        status: "전달완료",
        lastMessage: "전달 완료했습니다.",
        updatedAt: "어제",
        unreadCount: 0,
        messages: [
            { id: 1, sender: "me", text: "확인했습니다. 감사합니다!" },
            { id: 2, sender: "other", text: "전달 완료했습니다." },
        ],
    },
];
