import { AuthProvider } from "react-oidc-context";
import type { PropsWithChildren } from "react";

/**
 * OIDC via the Keycloak realm (ADR-016). The authority is a build-time env so
 * the same bundle works behind the nginx same-origin proxy (compose) and
 * against a locally started Keycloak (Vite dev).
 */
const authority = import.meta.env.VITE_OIDC_AUTHORITY ?? "http://localhost:8180/realms/acme-claims";

const oidcConfig = {
  url: authority,
  client_id: "claims-web",
  redirect_uri: `${window.location.origin}/`,
  post_logout_redirect_uri: window.location.origin,
  scope: "openid profile roles",
  response_type: "code",
  onSigninCallback: () => {
    window.history.replaceState({}, document.title, window.location.pathname);
  },
};

export function AppAuthProvider({ children }: PropsWithChildren) {
  return <AuthProvider {...oidcConfig}>{children}</AuthProvider>;
}
