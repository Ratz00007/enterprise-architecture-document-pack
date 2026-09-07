import axios from "axios";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useClaimsApi } from "./client";
import type { Claim, ClaimStatus, Page } from "../types";

export interface CreateClaimInput {
  policyNumber: string;
  claimType: string;
  incidentDate: string;
  description: string;
  estimatedAmountMinor: number | null;
}

function toApiError(error: unknown): string {
  if (axios.isAxiosError(error)) {
    const data = error.response?.data as { message?: string } | undefined;
    return data?.message ?? error.message;
  }
  return String(error);
}

export function useClaims(status: ClaimStatus | null) {
  const api = useClaimsApi();
  return useQuery({
    queryKey: ["claims", status],
    queryFn: async () => {
      const response = await api.get<Page<Claim>>("/claims", {
        params: status ? { status, size: 50 } : { size: 50 },
      });
      return response.data;
    },
  });
}

export function useClaim(id: string) {
  const api = useClaimsApi();
  return useQuery({
    queryKey: ["claim", id],
    queryFn: async () => {
      const response = await api.get<Claim>(`/claims/${id}`);
      return response.data;
    },
  });
}

export function useCreateClaim() {
  const api = useClaimsApi();
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (input: CreateClaimInput) => {
      const response = await api.post<Claim>("/claims", input);
      return response.data;
    },
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["claims"] }),
    onError: (error) => window.alert(`FNOL failed: ${toApiError(error)}`),
  });
}

export function useTransition(id: string) {
  const api = useClaimsApi();
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async ({ action, body }: { action: string; body?: unknown }) => {
      const response = await api.post<Claim>(`/claims/${id}/${action}`, body ?? {});
      return response.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["claim", id] });
      queryClient.invalidateQueries({ queryKey: ["claims"] });
    },
    onError: (error) => window.alert(`Transition failed: ${toApiError(error)}`),
  });
}
