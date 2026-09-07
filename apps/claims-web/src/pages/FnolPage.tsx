import { useForm } from "react-hook-form";
import { Link, useNavigate } from "react-router-dom";
import { useCreateClaim } from "../api/claims";
import { toMinorUnits } from "../lib/money";
import type { ClaimType } from "../types";

const CLAIM_TYPES: ClaimType[] = [
  "AUTO",
  "PROPERTY",
  "HEALTH",
  "LIFE",
  "LIABILITY",
  "WORKERS_COMP",
  "OTHER",
];

interface FnolForm {
  policyNumber: string;
  claimType: ClaimType;
  incidentDate: string;
  description: string;
  estimatedAmount: string;
}

export function FnolPage() {
  const createClaim = useCreateClaim();
  const navigate = useNavigate();
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<FnolForm>({
    defaultValues: { claimType: "AUTO", estimatedAmount: "" },
  });

  const onSubmit = handleSubmit((data) => {
    const estimated = data.estimatedAmount ? toMinorUnits(Number(data.estimatedAmount)) : null;
    createClaim.mutate(
      {
        policyNumber: data.policyNumber,
        claimType: data.claimType,
        incidentDate: new Date(data.incidentDate).toISOString(),
        description: data.description,
        estimatedAmountMinor: estimated,
      },
      {
        onSuccess: (claim) => navigate(`/claims/${claim.id}`),
      },
    );
  });

  return (
    <div className="mx-auto max-w-2xl">
      <h1 className="mb-1 text-2xl font-bold">First Notice of Loss</h1>
      <p className="mb-6 text-sm text-slate-600">
        Creates a claim in FNOL state. All amounts are captured in major units and
        stored as minor units (ADR-022).
      </p>
      <form onSubmit={onSubmit} className="space-y-4 rounded-lg border border-slate-200 bg-white p-6">
        <div>
          <label className="block text-sm font-medium" htmlFor="policyNumber">
            Policy number
          </label>
          <input
            id="policyNumber"
            className="mt-1 w-full rounded border border-slate-300 px-3 py-2"
            {...register("policyNumber", { required: "Policy number is required" })}
          />
          {errors.policyNumber && <p className="mt-1 text-sm text-rose-600">{errors.policyNumber.message}</p>}
        </div>

        <div>
          <label className="block text-sm font-medium" htmlFor="claimType">
            Claim type
          </label>
          <select
            id="claimType"
            className="mt-1 w-full rounded border border-slate-300 px-3 py-2"
            {...register("claimType")}
          >
            {CLAIM_TYPES.map((type) => (
              <option key={type} value={type}>
                {type}
              </option>
            ))}
          </select>
        </div>

        <div>
          <label className="block text-sm font-medium" htmlFor="incidentDate">
            Date and time of incident
          </label>
          <input
            id="incidentDate"
            type="datetime-local"
            className="mt-1 w-full rounded border border-slate-300 px-3 py-2"
            {...register("incidentDate", { required: "Incident date is required" })}
          />
          {errors.incidentDate && <p className="mt-1 text-sm text-rose-600">{errors.incidentDate.message}</p>}
        </div>

        <div>
          <label className="block text-sm font-medium" htmlFor="estimatedAmount">
            Estimated amount (major units, optional)
          </label>
          <input
            id="estimatedAmount"
            type="number"
            step="0.01"
            min="0"
            className="mt-1 w-full rounded border border-slate-300 px-3 py-2"
            {...register("estimatedAmount")}
          />
        </div>

        <div>
          <label className="block text-sm font-medium" htmlFor="description">
            What happened?
          </label>
          <textarea
            id="description"
            rows={4}
            className="mt-1 w-full rounded border border-slate-300 px-3 py-2"
            {...register("description")}
          />
        </div>

        <div className="flex items-center justify-between">
          <Link to="/" className="text-sm text-slate-600 hover:underline">
            Cancel
          </Link>
          <button
            type="submit"
            disabled={isSubmitting}
            className="rounded bg-slate-900 px-4 py-2 font-medium text-white hover:bg-slate-700 disabled:opacity-50"
          >
            Submit FNOL
          </button>
        </div>
      </form>
    </div>
  );
}
