import { apiRequest } from "./client";
import { API_ENDPOINTS } from "./endpoints";

export interface SendMailRequest {
    mail: string;
    student: boolean;
}

export interface ConfirmMailRequest {
    mail: string;
    code: string;
}

export interface LoginRequest {
    mail: string;
    password: string;
}

export interface TokenResponse {
    accessToken: string;
    refreshToken: string;
}

export interface FindIdRequest {
    name: string;
    phoneNumber: string;
}

export interface FindPasswordRequest extends FindIdRequest {
    mail: string;
}

export interface SignupRequest {
    mail: string;
    password: string;
    nickname: string;
    name: string;
    phoneNumber: string;
    birth: string;
    sourceStationId: number;
    destinationStationId: number;
    wayPoints?: number[];
    point?: number;
    picture?: string;
}

export interface EditPasswordRequest {
    nowPassword: string;
    editPassword: string;
}

export const authApi = {
    sendMail(request: SendMailRequest) {
        return apiRequest<null>({
            method: "POST",
            url: API_ENDPOINTS.auth.sendMail,
            data: request,
        });
    },

    confirmMail(request: ConfirmMailRequest) {
        return apiRequest<null>({
            method: "POST",
            url: API_ENDPOINTS.auth.confirmMail,
            data: request,
        });
    },

    confirmUniversityMail(request: ConfirmMailRequest) {
        return apiRequest<null>({
            method: "POST",
            url: API_ENDPOINTS.auth.confirmUniversityMail,
            data: request,
        });
    },

    signup(request: SignupRequest) {
        return apiRequest<null>({
            method: "POST",
            url: API_ENDPOINTS.auth.signup,
            data: request,
        });
    },

    login(request: LoginRequest) {
        return apiRequest<TokenResponse>({
            method: "POST",
            url: API_ENDPOINTS.auth.login,
            data: request,
        });
    },

    logout() {
        return apiRequest<null>({
            method: "DELETE",
            url: API_ENDPOINTS.auth.logout,
        });
    },

    reissue(refreshToken: string) {
        return apiRequest<TokenResponse>({
            method: "POST",
            url: API_ENDPOINTS.auth.reissue,
            data: { refreshToken },
        });
    },

    findId(request: FindIdRequest) {
        return apiRequest<null>({
            method: "POST",
            url: API_ENDPOINTS.auth.findId,
            data: request,
        });
    },

    findPassword(request: FindPasswordRequest) {
        return apiRequest<null>({
            method: "POST",
            url: API_ENDPOINTS.auth.findPassword,
            data: request,
        });
    },
    
    checkNicknameAvailable(nickname: string) {
        return apiRequest<boolean>({
            method: "GET",
            url: API_ENDPOINTS.auth.availableNickname,
            params: {
                nickname,
            },
        });
    },

    checkEmailAvailable(mail: string) {
        return apiRequest<boolean>({
            method: "GET",
            url: API_ENDPOINTS.auth.availableMail,
            params: {
                mail,
            },
        });
    },

    editPassword(request: EditPasswordRequest) {
        return apiRequest<boolean>({
            method: "PATCH",
            url: API_ENDPOINTS.account.editPassword,
            data: request
        })
    }
};
