import type { PropsWithChildren } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "react-oidc-context";

const STATUS_COLORS: Record<string, string> = {
  FNOL: "bg-slate-200 text-slate-800",
  TRIAGE: "bg-amber-200 text-amber-900",
  ADJUDICATION: "bg-blue-200 text-blue-900",
  APPROVED: "bg-emerald-200 text-emerald-900",
  REJECTED: "bg-rose-200 text-rose-900",
  PAYOUT: "bg-violet-200 text-violet-900",
  PAID: "bg-teal-200 text-teal-900",
  CLOSED: "bg-slate-300 text-slate-700",
};

export function ClaimStatusBadge({ status }: { status: string }) {
  return (
    <span className={`rounded-full px-2.5 py-0.5 text-xs font-semibold ${STATUS_COLORS[status] ?? "bg-slate-200"}`}>
      {status}
    </span>
  );
}

export function Layout({ children }: PropsWithChildren) {
  const auth = useAuth();
  const navigate = useNavigate();

  return (
    <div className="min-h-screen">
      <header className="flex items-center justify-between border-b border-slate-200 bg-white px-6 py-3">
        <div className="flex items-center gap-6">
          <Link to="/" className="text-lg font-bold tracking-tight">
            Acme Claims
          </Link>
          <nav className="flex gap-4 text-sm">
            <Link to="/" className="hover:underline">
              Claims
            </Link>
            <Link to="/fnol" className="hover:underline">
              Report FNOL
            </Link>
          </nav>
        </div>
        <div className="flex items-center gap-3 text-sm">
          {auth.isAuthenticated ? (
            <>
              <span className="text-slate-600">
                {auth.user?.profile.preferred_username ?? auth.user?.profile.sub}
              </span>
              <button
                className="rounded border border-slate-300 px-3 py-1 hover:bg-slate-100"
                onClick={() => {
                  void auth.removeUser().then(() => navigate("/"));
                }}
              >
                Sign out
              </button>
            </>
          ) : (
            <button
              className="rounded bg-slate-900 px-3 py-1 font-medium text-white hover:bg-slate-700"
              onClick={() => void auth.signinRedirect()}
            >
              Sign in
            </button>
          )}
        </div>
      </header>
      <main className="mx-auto max-w-5xl px-6 py-8">{children}</main>
    </div>
  );
}
