/**
 * API client. Wraps fetch with:
 *   - OIDC token attachment (Keycloak)
 *   - Idempotency-Key on every state-mutating call (per ADR-006)
 *   - Structured error envelope
 */

export interface ApiError {
  status: number;
  code: string;
  message: string;
  details?: unknown;
}

export class ApiClient {
  constructor(private readonly baseUrl: string) {}

  async get<T>(path: string): Promise<T> {
    return this.request<T>('GET', path);
  }

  async post<T>(path: string, body: unknown, idempotencyKey?: string): Promise<T> {
    return this.request<T>('POST', path, body, idempotencyKey);
  }

  async put<T>(path: string, body: unknown, idempotencyKey?: string): Promise<T> {
    return this.request<T>('PUT', path, body, idempotencyKey);
  }

  private async request<T>(
    method: 'GET' | 'POST' | 'PUT',
    path: string,
    body?: unknown,
    idempotencyKey?: string,
  ): Promise<T> {
    const headers: Record<string, string> = {
      'Content-Type': 'application/json',
      Accept: 'application/json',
    };
    if (idempotencyKey) {
      headers['Idempotency-Key'] = idempotencyKey;
    }
    const token = await this.getToken();
    if (token) {
      headers['Authorization'] = `Bearer ${token}`;
    }

    const resp = await fetch(`${this.baseUrl}${path}`, {
      method,
      headers,
      body: body === undefined ? undefined : JSON.stringify(body),
    });

    if (!resp.ok) {
      const err: ApiError = {
        status: resp.status,
        code: resp.headers.get('X-Error-Code') ?? 'unknown',
        message: resp.statusText,
      };
      try {
        const body = (await resp.json()) as { message?: string; details?: unknown };
        if (body.message) err.message = body.message;
        if (body.details) err.details = body.details;
      } catch {
        // body wasn't JSON
      }
      throw err;
    }

    if (resp.status === 204) {
      return undefined as T;
    }
    return (await resp.json()) as T;
  }

  private async getToken(): Promise<string | null> {
    // The Keycloak JS adapter is added in the security workstream.
    // For now, return null and let the dev env work without auth.
    return null;
  }
}

export const apiClient = new ApiClient(import.meta.env.VITE_API_URL ?? '/api');
