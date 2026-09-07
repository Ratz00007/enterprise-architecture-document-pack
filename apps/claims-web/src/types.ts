export type ClaimStatus =
  | "FNOL"
  | "TRIAGE"
  | "ADJUDICATION"
  | "APPROVED"
  | "REJECTED"
  | "PAYOUT"
  | "PAID"
  | "CLOSED";

export type ClaimType =
  | "AUTO"
  | "PROPERTY"
  | "HEALTH"
  | "LIFE"
  | "LIABILITY"
  | "WORKERS_COMP"
  | "OTHER";

export interface Claim {
  id: string;
  claimNumber: string;
  policyNumber: string;
  status: ClaimStatus;
  claimType: ClaimType;
  incidentDate: string;
  reportedDate: string;
  description: string | null;
  estimatedAmountMinor: number | null;
  approvedAmountMinor: number | null;
  paidAmountMinor: number | null;
  assignedTo: string | null;
  triagePriority: number | null;
  triageScore: number | null;
  adjudicationNotes: string | null;
  rejectionReason: string | null;
  payoutReference: string | null;
  payoutDate: string | null;
  version: number | null;
  createdAt: string | null;
  updatedAt: string | null;
  createdBy: string | null;
  updatedBy: string | null;
}

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}
