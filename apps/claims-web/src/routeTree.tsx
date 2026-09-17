/* The route tree. Generated-style file (kept hand-written for
   clarity until we wire up the TanStack Router CLI codegen). */
import { createRootRoute, createRoute } from '@tanstack/react-router';
import { ClaimListPage } from './pages/ClaimListPage';

export const rootRoute = createRootRoute();

export const indexRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: '/',
  component: () => <ClaimListPage />,
});

export const routeTree = rootRoute.addChildren([indexRoute]);
