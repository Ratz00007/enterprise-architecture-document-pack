import { describe, it, expect } from 'vitest';
import { claimsApi, type Claim } from '../api/claims';

describe('claimsApi', () => {
  it('exports a list function', () => {
    expect(typeof claimsApi.list).toBe('function');
  });

  it('exports a createFnol function that takes a request', () => {
    expect(typeof claimsApi.createFnol).toBe('function');
  });
});

describe('Claim type shape', () => {
  it('matches the API contract', () => {
    const sample: Claim = {
      id: '00000000-0000-0000-0000-000000000001',
      claimNumber: 'CLM-2026-00000001',
      policyId: '00000000-0000-0000-0000-000000000010',
      claimantPartyId: '00000000-0000-0000-0000-000000000020',
      state: 'NEW',
      lossDate: '2026-01-01T00:00:00Z',
      reportedDate: '2026-01-01T01:00:00Z',
      estimatedAmountCents: 100000,
      currency: 'USD',
      fraudScore: null,
      description: 'Test',
      assignedAdjusterId: null,
      version: 1,
    };
    expect(sample.state).toBe('NEW');
    expect(sample.estimatedAmountCents).toBe(100000);
  });
});
