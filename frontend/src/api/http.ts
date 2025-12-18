export type ApiError = {
    status: number;
    message: string;
    details?: unknown;
};

export type HttpOptions = {
    method?: string;
    headers?: HeadersInit;
    body?: any;
    signal?: AbortSignal;
};

let unauthorizedHandler: (() => void) | null = null;

export function setUnauthorizedHandler(handler: (() => void) | null) {
    unauthorizedHandler = handler;
}

function buildInit(options: HttpOptions): RequestInit {
    const { body, method, headers, ...rest } = options;
    const init: RequestInit = {
        method: method || (body ? "POST" : "GET"),
        credentials: "include",
        headers: {
            ...(!body || body instanceof FormData ? {} : { "Content-Type": "application/json" }),
            ...headers,
        },
        ...rest,
    };

    if (body === undefined || body === null) {
        return init;
    }
    if (body instanceof FormData) {
        init.body = body;
    } else {
        init.body = JSON.stringify(body);
    }
    return init;
}

async function parseResponse(res: Response) {
    const text = await res.text();
    if (!text) return null;
    try {
        return JSON.parse(text);
    } catch {
        return text;
    }
}

export async function request<T>(url: string, options: HttpOptions = {}): Promise<T> {
    const init = buildInit(options);
    const res = await fetch(url, init);
    const data = await parseResponse(res);
    if (!res.ok) {
        if (res.status === 401 && unauthorizedHandler) {
            unauthorizedHandler();
        }
        const error: ApiError = {
            status: res.status,
            message: (data as any)?.message || res.statusText || "Request failed",
            details: data,
        };
        throw error;
    }
    return data as T;
}
