export interface ApiResponse<T> {
    success?: boolean;
    isSuccess?: boolean;
    code: string;
    message: string;
    result: T;
}

export interface ApiErrorBody {
    success?: boolean;
    isSuccess?: boolean;
    code?: string;
    message?: string;
    result?: unknown;
}

export class ApiError extends Error {
    readonly status?: number;
    readonly code?: string;
    readonly details?: unknown;

    constructor({
        message,
        status,
        code,
        details,
    }: {
        message: string;
        status?: number;
        code?: string;
        details?: unknown;
    }) {
        super(message);
        this.name = "ApiError";
        this.status = status;
        this.code = code;
        this.details = details;
    }
}
