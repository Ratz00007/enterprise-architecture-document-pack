/**
 * Claims API. Mirrors the backend endpoints in apps/claims-api/.
 * See docs/api/openapi.yaml (TBD) for the full surface.
 */
import { apiClient } from './client';

export type ClaimState =
  | 'NEW'
  | 'TRIAGE'
  | 'INVESTIGATING'
  | 'DECISION_PENDING'
  | 'APPROVED'
  | 'RESERVED'
  | 'PAYOUT_PENDING'
  | 'PAID'
  | 'DENIED'
  | 'WITHDRAWN'
  | 'CLOSED'
  | 'ARCHIVED'
  | 'REOPENED';

export interface Claim {
  id: string;
  claimNumber: string;
  policyId: string;
  claimantPartyId: string;
  state: ClaimState;
  lossDate: string; // ISO 8601
  reportedDate: string; // ISO 8601
  estimatedAmountCents: number;
  currency: string;
  fraudScore: number | null;
  description: string;
  assignedAdjusterId: string | null;
  version: number;
}

export interface FnolRequest {
  policyId: string;
  claimantPartyId: string;
  lossType: 'fire' | 'water' | 'theft' | 'wind' | 'liability' | 'auto_collision' | 'other';
  lossDate: string;
  description: string;
  estimatedAmountCents: number;
  currency: string;
}

export const claimsApi = {
  list: (filter?: { state?: ClaimState; assignedToMe?: boolean }) =>
    apiClient.get<Claim[]>(`/v1/claims${toQueryString(filter)}`),

  get: (id: string) => apiClient.get<Claim>(`/v1/claims/${id}`),

  createFnol: (req: FnolRequest) =>
    apiClient.post<Claim>('/v1/claims/fnol', req, cryptoRandomIdempotencyKey()),

  transition: (id: string, toState: ClaimState, comment: string) =>
    apiClient.post<Claim>(
      `/v1/claims/${id}/transitions`,
      { toState, comment },
      cryptoRandomIdempotencyKey(),
    ),
};

function toQueryString(obj?: Record<string, unknown>): string {
  if (!obj) return '';
  const params = new URLSearchParams();
  for (const [k, v] of Object.entries(obj)) {
    if (v !== undefined && v !== null) params.append(k, String(v));
  }
  const s = params.toString();
  return s ? `?${s}` : '';
}

function cryptoRandomIdempotencyKey(): string {
  return crypto.randomUUID();
}
