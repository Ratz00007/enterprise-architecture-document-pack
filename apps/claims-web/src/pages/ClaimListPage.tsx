import { useQuery } from '@tanstack/react-query';
import { claimsApi } from '../api/claims';
import { logEvent } from '../observability';

export function ClaimListPage(): JSX.Element {
  const { data, error, isLoading } = useQuery({
    queryKey: ['claims', 'list'],
    queryFn: () => claimsApi.list(),
  });

  if (isLoading) return <p>Loading…</p>;
  if (error) return <p role="alert">Failed to load claims.</p>;
  if (!data || data.length === 0) return <p>No claims yet.</p>;

  logEvent('claims.list.viewed', { count: data.length });

  return (
    <main>
      <h1>Claims</h1>
      <table>
        <thead>
          <tr>
            <th scope="col">Claim #</th>
            <th scope="col">State</th>
            <th scope="col">Loss date</th>
            <th scope="col">Estimated</th>
            <th scope="col">Adjuster</th>
          </tr>
        </thead>
        <tbody>
          {data.map((c) => (
            <tr key={c.id}>
              <td>{c.claimNumber}</td>
              <td>{c.state}</td>
              <td>{c.lossDate}</td>
              <td>
                {(c.estimatedAmountCents / 100).toLocaleString(undefined, {
                  style: 'currency',
                  currency: c.currency,
                })}
              </td>
              <td>{c.assignedAdjusterId ?? '—'}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </main>
  );
}
