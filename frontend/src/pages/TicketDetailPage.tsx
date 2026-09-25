import { useState } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { Link, useParams } from 'react-router-dom'
import { ApiError } from '../api/ApiError'
import { getTicket, updateTicket } from '../api/tickets'
import type { TicketPriority, TicketResponse } from '../api/types'
import { CommentList } from '../components/CommentList'
import { ErrorBanner } from '../components/ErrorBanner'
import { StatusActions } from '../components/StatusActions'
import { StatusBadge } from '../components/StatusBadge'
import { StatusHistoryList } from '../components/StatusHistoryList'
import { TicketForm, type TicketFormValues } from '../components/TicketForm'
import { isTerminalStatus } from '../constants'
import { formatDateTime } from '../utils/dates'

export function TicketDetailPage() {
  const { id: idParam } = useParams()
  const ticketId = Number(idParam)
  const queryClient = useQueryClient()
  const [editing, setEditing] = useState(false)
  const [saveError, setSaveError] = useState<ApiError | null>(null)
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})
  const [submitting, setSubmitting] = useState(false)

  const {
    data: ticket,
    isLoading,
    isError,
    error,
    refetch,
  } = useQuery({
    queryKey: ['ticket', ticketId],
    queryFn: () => getTicket(ticketId),
    enabled: Number.isFinite(ticketId),
  })

  function setTicketInCache(updated: TicketResponse) {
    queryClient.setQueryData(['ticket', ticketId], updated)
  }

  async function handleSave(values: TicketFormValues) {
    if (!ticket) return
    setSubmitting(true)
    setSaveError(null)
    setFieldErrors({})
    const payload: Record<string, unknown> = { version: ticket.version }
    if (values.title !== ticket.title) payload.title = values.title
    if (values.description !== ticket.description) payload.description = values.description
    if (values.priority !== ticket.priority) payload.priority = values.priority
    const assignee = values.assignee.trim() || null
    if (assignee !== (ticket.assignee ?? '')) payload.assignee = assignee

    try {
      const updated = await updateTicket(ticket.id, payload as {
        title?: string
        description?: string
        priority?: TicketPriority
        assignee?: string | null
        version: number
      })
      setTicketInCache(updated)
      setEditing(false)
    } catch (e) {
      const err = e instanceof ApiError ? e : ApiError.network()
      setSaveError(err)
      if (err.fieldErrors.length) {
        const map: Record<string, string> = {}
        err.fieldErrors.forEach((fe) => {
          map[fe.field] = fe.message
        })
        setFieldErrors(map)
      }
      if (err.code === 'CONCURRENT_MODIFICATION' || err.code === 'TICKET_NOT_EDITABLE') {
        void refetch()
      }
    } finally {
      setSubmitting(false)
    }
  }

  if (!Number.isFinite(ticketId)) {
    return <p>Invalid ticket id.</p>
  }

  if (isLoading) return <p className="page">Loading…</p>

  if (isError) {
    const err = error instanceof ApiError ? error : ApiError.network()
    if (err.code === 'TICKET_NOT_FOUND' || err.status === 404) {
      return (
        <div className="page narrow">
          <h1>Ticket not found</h1>
          <p>Ticket #{ticketId} does not exist.</p>
          <Link to="/tickets">Back to tickets</Link>
        </div>
      )
    }
    return (
      <div className="page">
        <ErrorBanner message={err.detail} onRetry={() => void refetch()} />
      </div>
    )
  }

  if (!ticket) return null

  const terminal = isTerminalStatus(ticket.status)

  return (
    <div className="page">
      <Link to="/tickets" className="back-link">← Back to tickets</Link>
      <header className="detail-header">
        <div>
          <h1>
            <span className="muted">#{ticket.id}</span> {ticket.title}
          </h1>
          <StatusBadge status={ticket.status} />
        </div>
        {!terminal && !editing && (
          <button type="button" className="btn btn-secondary" onClick={() => setEditing(true)}>
            Edit
          </button>
        )}
      </header>
      <p className="meta">
        Priority: <strong>{ticket.priority}</strong>
        {' · '}
        Assignee: <strong>{ticket.assignee ?? 'Unassigned'}</strong>
        {' · '}
        Created: {formatDateTime(ticket.createdAt)}
      </p>
      {editing ? (
        <>
          {saveError && !saveError.fieldErrors.length && (
            <ErrorBanner
              message={saveError.detail}
              onReload={
                saveError.code === 'CONCURRENT_MODIFICATION' ? () => void refetch() : undefined
              }
            />
          )}
          <TicketForm
            initial={{
              title: ticket.title,
              description: ticket.description,
              priority: ticket.priority,
              assignee: ticket.assignee ?? '',
            }}
            submitLabel="Save"
            fieldErrors={fieldErrors}
            submitting={submitting}
            onSubmit={handleSave}
            onCancel={() => setEditing(false)}
          />
        </>
      ) : (
        <p className="description">{ticket.description}</p>
      )}
      {!terminal && (
        <StatusActions
          ticket={ticket}
          onUpdated={setTicketInCache}
          onReload={() => void refetch()}
        />
      )}
      <StatusHistoryList ticketId={ticket.id} />
      <CommentList ticket={ticket} onTicketRefetch={() => void refetch()} />
    </div>
  )
}
