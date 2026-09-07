/** Next action(s) the lifecycle allows for a claim in the given state (ADR-021). */
import type { ClaimStatus } from "../types";

export interface TransitionAction {
  action: "triage" | "adjudication" | "approve" | "reject" | "payout" | "payout/complete" | "close";
  label: string;
  needsBody: boolean;
}

export function allowedActions(status: ClaimStatus): TransitionAction[] {
  switch (status) {
    case "FNOL":
      return [{ action: "triage", label: "Start triage", needsBody: true }];
    case "TRIAGE":
      return [{ action: "adjudication", label: "Begin adjudication", needsBody: true }];
    case "ADJUDICATION":
      return [
        { action: "approve", label: "Approve", needsBody: true },
        { action: "reject", label: "Reject", needsBody: true },
      ];
    case "APPROVED":
      return [{ action: "payout", label: "Initiate payout", needsBody: true }];
    case "PAYOUT":
      return [{ action: "payout/complete", label: "Complete payout", needsBody: false }];
    case "PAID":
      return [{ action: "close", label: "Close claim", needsBody: false }];
    case "REJECTED":
      return [{ action: "close", label: "Close claim", needsBody: false }];
    case "CLOSED":
      return [];
  }
}
