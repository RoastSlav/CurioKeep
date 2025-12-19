import { ApiError } from "./errors";

type AuthEvent = "AUTH_REQUIRED";

type AuthEventListener = (event: AuthEvent) => void;

type ApiRequestOptions = {
  method?: string;
  headers?: HeadersInit;
  body?: any;
  signal?: AbortSignal;
};

const authListeners = new Set<AuthEventListener>();

export function subscribeAuthEvents(listener: AuthEventListener): () => void {
  authListeners.add(listener);
  return () => authListeners.delete(listener);
}

export function emitAuthRequired() {
  authListeners.forEach((listener) => listener("AUTH_REQUIRED"));
}

function normalizeUrl(path: string): string {
  if (path.startsWith("http://") || path.startsWith("https://")) return path;
  const normalized = path.startsWith("/") ? path : `/${path}`;
  return `/api${normalized}`;
}

function readCsrfToken(): string | null {
  if (typeof document === "undefined") return null;
  const match = document.cookie.match(/(?:^|; )XSRF-TOKEN=([^;]+)/);
  return match ? decodeURIComponent(match[1]) : null;
}

function buildInit(options: ApiRequestOptions): RequestInit {
  const { method, headers, body, ...rest } = options;
  const finalMethod = method || (body ? "POST" : "GET");
  const defaultHeaders: HeadersInit = {};

  if (body && !(body instanceof FormData)) {
    defaultHeaders["Content-Type"] = "application/json";
  }

  if (finalMethod && finalMethod !== "GET" && finalMethod !== "HEAD") {
    const csrf = readCsrfToken();
    if (csrf) {
      defaultHeaders["X-XSRF-TOKEN"] = csrf;
    }
  }

  const init: RequestInit = {
    method: finalMethod,
    credentials: "include",
    headers: {
      ...defaultHeaders,
      ...headers,
    },
    ...rest,
  };

  if (body !== undefined && body !== null) {
    init.body = body instanceof FormData ? body : JSON.stringify(body);
  }

  return init;
}

async function parseBody(res: Response): Promise<any> {
  const text = await res.text();
  if (!text) return null;
  try {
    return JSON.parse(text);
  } catch {
    return text;
  }
}

export async function apiFetch<T>(
  path: string,
  options: ApiRequestOptions = {}
): Promise<T> {
  const url = normalizeUrl(path);
  const init = buildInit(options);
  const res = await fetch(url, init);
  const body = await parseBody(res);

  if (res.status === 401 || res.status === 403) {
    emitAuthRequired();
  }

  if (!res.ok) {
    const message =
      (body as any)?.message || res.statusText || "Request failed";
    throw new ApiError(res.status, message, body);
  }

  return body as T;
}
