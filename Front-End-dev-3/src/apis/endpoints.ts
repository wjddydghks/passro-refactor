export const API_ENDPOINTS = {
    auth: {
        sendMail: "/auth/mail/send",
        confirmMail: "/auth/mail/confirm",
        confirmUniversityMail: "/auth/mail/confirm/University",
        signup: "/auth/signup",
        login: "/auth/login",
        logout: "/auth/logout",
        reissue: "/auth/reissue",
        findId: "/auth/find/id",
        findPassword: "/auth/find/password",
        availableNickname: "/auth/nickname/check",
        availableMail: "/auth/mail/check",
        editPassword: "/mypage/edit/password",
    },
    subway: {
        search: "/subway/search",
        path: "/subway/routes/shortest",
    },
    sender: {
        root: "/sender",
        detail: (deliveryId: number) => `/sender/${deliveryId}`,
        shipperLocation: (deliveryId: number) =>
            `/sender/${deliveryId}/shipper-location`,
        payment: "/sender/payment",
        complete: (deliveryId: number) => `/sender/${deliveryId}/complete`,
        terms: (deliveryId: number) => `/sender/${deliveryId}/terms`,
        cancel: (deliveryId: number) => `/sender/${deliveryId}/cancel`,
        location: (deliveryId: number) =>
            `/sender/${deliveryId}/shipper-location`,
    },
    shipper: {
        matched: "/shipper/matched",
        root: "/shipper/",
        location: "/shipper/location",
        detail: (deliveryId: number) => `/shipper/${deliveryId}/`,
        acceptMatch: (deliveryId: number) => `/shipper/${deliveryId}/matched`,
        acquire: (deliveryId: number) => `/shipper/${deliveryId}/acquire`,
        confirm: (deliveryId: number) => `/shipper/${deliveryId}/confirm`,
    },
    file: {
        imageUploadUrl: "/file/image/upload-url",
        imageDownloadUrl: "/file/image/download-url",
    },
    report: {
        root: "/reports",
        getReportList: "/reports/me",
    },
    review: {
        root: "/reviews",
        average: (userId: number) => `/reviews/average/${userId}`,
    },
    inquiry: {
        root: "/inquiry",
    },
    deliveryInquiry: {
        root: "/delivery-inquiry",
        byDelivery: (deliveryId: number) => `/delivery-inquiry/${deliveryId}`,
    },
    account: {
        profile: "/mypage/shipper",
        editMyInfo: "/mypage/edit/myInfo",
        sendPasswordEditMail: "/mypage/edit/password/mail",
        editPassword: "/mypage/edit/password",
        points: "/account/points",
        checkStudent: "/mypage/student-certification",
    },
    chat: {
        messages: (deliveryId: number) => `/chat/${deliveryId}/messages`,
        info: (deliveryId: number) => `/chat/${deliveryId}/info`,
        unreadCount: (deliveryId: number) =>
            `/chat/${deliveryId}/unread-count`,
        rooms: "/chat/rooms",
        exit: (deliveryId: number) => `/chat/${deliveryId}`
    },
    notification: {
        root: "/notifications",
        unreadCount: "/notifications/unread-count",
        read: (notificationId: number) =>
            `/notifications/${notificationId}/read`,
        detail: (notificationId: number) => `/notifications/${notificationId}`,
    },
    market: {
        root: "/market",
        purchase: (marketId: number) => `/market/${marketId}/purchase`,
    },
} as const;
