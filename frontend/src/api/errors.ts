export class ApiError extends Error {
    status: number;
    body: unknown;

    constructor(status: number, message: string, body?: unknown) {
        super(message);
        this.status = status;
        this.body = body;
        Object.setPrototypeOf(this, ApiError.prototype);
    }
}

export function isApiError(error: unknown): error is ApiError {
    return error instanceof ApiError;
}
