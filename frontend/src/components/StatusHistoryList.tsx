import { useQuery } from '@tanstack/react-query'
import { listStatusHistory } from '../api/tickets'
import { formatDateTime } from '../utils/dates'
import { ErrorBanner } from './ErrorBanner'

export function StatusHistoryList({ ticketId }: { ticketId: number }) {
  const { data, isLoading, isError, refetch } = useQuery({
    queryKey: ['history', ticketId],
    queryFn: () => listStatusHistory(ticketId),
  })

  if (isLoading) return <p className="muted">Loading history…</p>
  if (isError) {
    return (
      <ErrorBanner
        message="Could not load status history."
        onRetry={() => void refetch()}
      />
    )
  }

  return (
    <section className="panel" aria-label="Status history">
      <h2>Status history</h2>
      <ul className="history-list">
        {data?.map((row, i) => (
          <li key={`${row.changedAt}-${i}`}>
            <span className="history-transition">
              {row.fromStatus ? `${row.fromStatus} → ${row.toStatus}` : `— → ${row.toStatus}`}
            </span>
            <span className="muted"> · {formatDateTime(row.changedAt)}</span>
            {row.note && <p className="history-note muted">{row.note}</p>}
          </li>
        ))}
      </ul>
    </section>
  )
}
