import { useState } from "react";
import { Link } from "react-router-dom";
import { useClaims } from "../api/claims";
import { ClaimStatusBadge } from "../components/Layout";
import type { ClaimStatus } from "../types";

const STATUSES: ClaimStatus[] = [
  "FNOL",
  "TRIAGE",
  "ADJUDICATION",
  "APPROVED",
  "REJECTED",
  "PAYOUT",
  "PAID",
  "CLOSED",
];

export function ClaimsListPage() {
  const [statusFilter, setStatusFilter] = useState<ClaimStatus | null>(null);
  const claims = useClaims(statusFilter);

  return (
    <div>
      <div className="mb-4 flex items-center justify-between">
        <h1 className="text-2xl font-bold">Claims</h1>
        <select
          className="rounded border border-slate-300 px-3 py-1.5 text-sm"
          value={statusFilter ?? ""}
          onChange={(event) => setStatusFilter(event.target.value ? (event.target.value as ClaimStatus) : null)}
        >
          <option value="">All statuses</option>
          {STATUSES.map((status) => (
            <option key={status} value={status}>
              {status}
            </option>
          ))}
        </select>
      </div>

      {claims.isLoading && <p className="text-slate-600">Loading…</p>}
      {claims.error && <p className="text-rose-600">Failed to load claims. Is the API running?</p>}
      {claims.data && claims.data.content.length === 0 && (
        <p className="text-slate-600">No claims yet. Report a FNOL to get started.</p>
      )}

      {claims.data && claims.data.content.length > 0 && (
        <table className="w-full border-collapse rounded-lg border border-slate-200 bg-white text-sm">
          <thead>
            <tr className="border-b border-slate-200 text-left text-slate-600">
              <th className="px-4 py-2">Claim number</th>
              <th className="px-4 py-2">Policy</th>
              <th className="px-4 py-2">Type</th>
              <th className="px-4 py-2">Status</th>
              <th className="px-4 py-2 text-right">Estimated</th>
              <th className="px-4 py-2">Reported</th>
            </tr>
          </thead>
          <tbody>
            {claims.data.content.map((claim) => (
              <tr key={claim.id} className="border-b border-slate-100 hover:bg-slate-50">
                <td className="px-4 py-2 font-mono">
                  <Link to={`/claims/${claim.id}`} className="hover:underline">
                    {claim.claimNumber}
                  </Link>
                </td>
                <td className="px-4 py-2">{claim.policyNumber}</td>
                <td className="px-4 py-2">{claim.claimType}</td>
                <td className="px-4 py-2">
                  <ClaimStatusBadge status={claim.status} />
                </td>
                <td className="px-4 py-2 text-right">{claim.estimatedAmountMinor ?? "—"}</td>
                <td className="px-4 py-2">{claim.reportedDate?.slice(0, 10)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
}
