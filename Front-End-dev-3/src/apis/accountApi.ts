import { Profile, EditProfile, EditPassword } from "../types/user";
import { apiRequest } from "./client";
import { API_ENDPOINTS } from "./endpoints";

export const accountApi = {
    getProfile() {
        return apiRequest<Profile>({
            method: "GET",
            url: API_ENDPOINTS.account.profile,
        });
    },

    editProfile(request: EditProfile) {
        return apiRequest<null>({
            method: "PATCH",
            url: API_ENDPOINTS.account.editMyInfo,
            data: request,
        });
    },

    sendPasswordEditMail() {
        return apiRequest<null>({
            method: "PATCH",
            url: API_ENDPOINTS.account.sendPasswordEditMail,
        });
    },

    changePassword(request: EditPassword) {
        return apiRequest<null>({
            method: "PATCH",
            url: API_ENDPOINTS.account.editPassword,
            data: request,
        });
    },

    checkStudent() {
        return apiRequest<boolean>({
            method: "GET",
            url: API_ENDPOINTS.account.checkStudent
        });
    },
};
