import { useQuery } from '@tanstack/react-query'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { listTickets } from '../api/tickets'
import type { TicketStatus } from '../api/types'
import { ErrorBanner } from '../components/ErrorBanner'
import { LoadingRows } from '../components/LoadingRows'
import { StatusBadge } from '../components/StatusBadge'
import { TICKET_STATUSES } from '../constants'
import { useDebouncedValue } from '../hooks/useDebouncedValue'

export function TicketListPage() {
  const navigate = useNavigate()
  const [params, setParams] = useSearchParams()
  const qRaw = params.get('q') ?? ''
  const qDebounced = useDebouncedValue(qRaw, 300)
  const statusParam = params.get('status') ?? ''
  const pageUi = Math.max(1, Number(params.get('page') ?? '1') || 1)
  const apiPage = pageUi - 1

  const statusFilter = TICKET_STATUSES.includes(statusParam as TicketStatus)
    ? (statusParam as TicketStatus)
    : undefined

  const { data, isLoading, isError, refetch, isFetching } = useQuery({
    queryKey: ['tickets', qDebounced, statusFilter, apiPage],
    queryFn: () =>
      listTickets({
        q: qDebounced || undefined,
        status: statusFilter,
        page: apiPage,
      }),
  })

  function setSearch(updates: Record<string, string | null>) {
    const next = new URLSearchParams(params)
    Object.entries(updates).forEach(([k, v]) => {
      if (v == null || v === '') next.delete(k)
      else next.set(k, v)
    })
    setParams(next)
  }

  const showEmpty = !isLoading && data?.content.length === 0

  return (
    <div className="page">
      <header className="page-header">
        <h1>Support Tickets</h1>
        <Link to="/tickets/new" className="btn btn-primary">+ New ticket</Link>
      </header>
      <div className="filters">
        <label className="sr-only" htmlFor="search">Search title or description</label>
        <input
          id="search"
          type="search"
          placeholder="Search title or description…"
          value={qRaw}
          onChange={(e) => setSearch({ q: e.target.value, page: '1' })}
        />
        <label className="sr-only" htmlFor="status-filter">Status</label>
        <select
          id="status-filter"
          value={statusFilter ?? ''}
          onChange={(e) =>
            setSearch({ status: e.target.value || null, page: '1' })
          }
        >
          <option value="">Status: All</option>
          {TICKET_STATUSES.map((s) => (
            <option key={s} value={s}>{s.replace('_', ' ')}</option>
          ))}
        </select>
      </div>
      {isError && (
        <ErrorBanner
          message="Cannot reach the server. Check your connection and try again."
          onRetry={() => void refetch()}
        />
      )}
      <table className="ticket-table">
        <thead>
          <tr>
            <th>#</th>
            <th>Title</th>
            <th>Priority</th>
            <th>Status</th>
            <th>Assignee</th>
          </tr>
        </thead>
        <tbody>
          {isLoading || isFetching ? (
            <LoadingRows cols={5} />
          ) : (
            data?.content.map((t) => (
              <tr
                key={t.id}
                className="clickable-row"
                onClick={() => navigate(`/tickets/${t.id}`)}
                onKeyDown={(e) => {
                  if (e.key === 'Enter') navigate(`/tickets/${t.id}`)
                }}
                tabIndex={0}
                role="link"
              >
                <td>{t.id}</td>
                <td>{t.title}</td>
                <td><span className="priority-badge">{t.priority}</span></td>
                <td><StatusBadge status={t.status} /></td>
                <td className={t.assignee ? '' : 'muted'}>
                  {t.assignee ?? 'Unassigned'}
                </td>
              </tr>
            ))
          )}
        </tbody>
      </table>
      {showEmpty && (
        <div className="empty-state">
          <p>No tickets match your search.</p>
          <button
            type="button"
            className="btn-link"
            onClick={() => setParams(new URLSearchParams())}
          >
            Clear filters
          </button>
        </div>
      )}
      {data && data.totalPages > 0 && (
        <nav className="pagination" aria-label="Pagination">
          <button
            type="button"
            className="btn btn-secondary"
            disabled={pageUi <= 1}
            onClick={() => setSearch({ page: String(pageUi - 1) })}
          >
            ‹ Prev
          </button>
          <span>
            Page {pageUi} of {Math.max(1, data.totalPages)}
          </span>
          <button
            type="button"
            className="btn btn-secondary"
            disabled={pageUi >= data.totalPages}
            onClick={() => setSearch({ page: String(pageUi + 1) })}
          >
            Next ›
          </button>
        </nav>
      )}
    </div>
  )
}
