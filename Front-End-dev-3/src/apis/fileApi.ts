import axios from "axios";
import { apiRequest } from "./client";
import { API_ENDPOINTS } from "./endpoints";

export interface ImageUploadUrlRequest {
    fileName: string;
    contentType: string;
    fileSize: number;
}

export interface ImageUploadUrl {
    imageKey: string;
    uploadUrl: string;
}

export const fileApi = {
    getImageUploadUrl(request: ImageUploadUrlRequest) {
        return apiRequest<ImageUploadUrl>({
            method: "POST",
            url: API_ENDPOINTS.file.imageUploadUrl,
            data: request,
        });
    },

    getImageDownloadUrl(imageKey: string) {
        return apiRequest<string>({
            method: "GET",
            url: API_ENDPOINTS.file.imageDownloadUrl,
            params: { imageKey },
        });
    },

    async uploadToPresignedUrl(uploadUrl: string, file: File) {
        await axios.put(uploadUrl, file, {
            headers: {
                "Content-Type": file.type || "application/octet-stream",
            },
        });
    },
};
