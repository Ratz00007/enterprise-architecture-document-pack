import { useState } from "react";
import { Link, useParams } from "react-router-dom";
import { useClaim, useTransition } from "../api/claims";
import { ClaimStatusBadge } from "../components/Layout";
import { formatMinorUnits } from "../lib/money";
import { allowedActions } from "../lib/transitions";
import type { TransitionAction } from "../lib/transitions";

export function ClaimDetailPage() {
  const { id = "" } = useParams();
  const claim = useClaim(id);
  const transition = useTransition(id);
  const [triagePriority, setTriagePriority] = useState("3");
  const [triageScore, setTriageScore] = useState("50");
  const [notes, setNotes] = useState("");
  const [amount, setAmount] = useState("");
  const [reference, setReference] = useState("");

  if (claim.isLoading) return <p className="text-slate-600">Loading…</p>;
  if (claim.error || !claim.data)
    return <p className="text-rose-600">Claim not found. <Link to="/" className="underline">Back to list</Link></p>;

  const c = claim.data;

  const submit = (action: TransitionAction) => {
    const body =
      action.action === "triage"
        ? { priority: Number(triagePriority), score: Number(triageScore), assignedTo: null }
        : action.action === "adjudication"
          ? { notes }
          : action.action === "approve"
            ? { approvedAmountMinor: Math.round(Number(amount) * 100) }
            : action.action === "reject"
              ? { reason: notes }
              : action.action === "payout"
                ? { payoutReference: reference }
                : {};
    transition.mutate({ action: action.action, body });
  };

  return (
    <div>
      <div className="mb-4 flex items-center justify-between">
        <div>
          <h1 className="font-mono text-2xl font-bold">{c.claimNumber}</h1>
          <p className="text-sm text-slate-600">
            Policy {c.policyNumber} · {c.claimType} · reported {c.reportedDate?.slice(0, 10)}
          </p>
        </div>
        <ClaimStatusBadge status={c.status} />
      </div>

      <dl className="mb-6 grid grid-cols-2 gap-x-8 gap-y-2 rounded-lg border border-slate-200 bg-white p-6 text-sm md:grid-cols-4">
        <div><dt className="text-slate-500">Estimated</dt><dd className="font-medium">{formatMinorUnits(c.estimatedAmountMinor)}</dd></div>
        <div><dt className="text-slate-500">Approved</dt><dd className="font-medium">{formatMinorUnits(c.approvedAmountMinor)}</dd></div>
        <div><dt className="text-slate-500">Paid</dt><dd className="font-medium">{formatMinorUnits(c.paidAmountMinor)}</dd></div>
        <div><dt className="text-slate-500">Assigned to</dt><dd className="font-medium">{c.assignedTo ?? "—"}</dd></div>
        <div><dt className="text-slate-500">Triage priority</dt><dd className="font-medium">{c.triagePriority ?? "—"}</dd></div>
        <div><dt className="text-slate-500">Triage score</dt><dd className="font-medium">{c.triageScore ?? "—"}</dd></div>
        <div><dt className="text-slate-500">Payout reference</dt><dd className="font-medium">{c.payoutReference ?? "—"}</dd></div>
        <div><dt className="text-slate-500">Version</dt><dd className="font-medium">{c.version ?? "—"}</dd></div>
        {c.description && <div className="col-span-2 md:col-span-4"><dt className="text-slate-500">Description</dt><dd>{c.description}</dd></div>}
        {c.adjudicationNotes && <div className="col-span-2 md:col-span-4"><dt className="text-slate-500">Adjudication notes</dt><dd>{c.adjudicationNotes}</dd></div>}
        {c.rejectionReason && <div className="col-span-2 md:col-span-4"><dt className="text-slate-500">Rejection reason</dt><dd>{c.rejectionReason}</dd></div>}
      </dl>

      {allowedActions(c.status).map((action) => (
        <div key={action.action} className="mb-3 rounded-lg border border-slate-200 bg-white p-4">
          <h2 className="mb-2 font-semibold">{action.label}</h2>
          {action.action === "triage" && (
            <div className="mb-2 flex gap-3 text-sm">
              <label className="flex items-center gap-2">
                Priority
                <input type="number" min={1} max={5} value={triagePriority} onChange={(e) => setTriagePriority(e.target.value)}
                  className="w-20 rounded border border-slate-300 px-2 py-1" />
              </label>
              <label className="flex items-center gap-2">
                Score
                <input type="number" min={0} max={100} value={triageScore} onChange={(e) => setTriageScore(e.target.value)}
                  className="w-24 rounded border border-slate-300 px-2 py-1" />
              </label>
            </div>
          )}
          {(action.action === "adjudication" || action.action === "reject") && (
            <textarea rows={2} placeholder="Notes / reason" value={notes} onChange={(e) => setNotes(e.target.value)}
              className="mb-2 w-full rounded border border-slate-300 px-3 py-2 text-sm" />
          )}
          {action.action === "approve" && (
            <input type="number" step="0.01" min="0" placeholder="Approved amount (major units)" value={amount}
              onChange={(e) => setAmount(e.target.value)}
              className="mb-2 w-64 rounded border border-slate-300 px-3 py-2 text-sm" />
          )}
          {action.action === "payout" && (
            <input type="text" placeholder="Payout reference" value={reference} onChange={(e) => setReference(e.target.value)}
              className="mb-2 w-64 rounded border border-slate-300 px-3 py-2 text-sm" />
          )}
          <button
            onClick={() => submit(action)}
            disabled={transition.isPending || (action.action === "reject" && !notes)}
            className="rounded bg-slate-900 px-4 py-1.5 text-sm font-medium text-white hover:bg-slate-700 disabled:opacity-50"
          >
            {action.label}
          </button>
        </div>
      ))}

      {c.status === "CLOSED" && <p className="text-slate-600">This claim is closed. No further transitions are possible.</p>}
    </div>
  );
}
