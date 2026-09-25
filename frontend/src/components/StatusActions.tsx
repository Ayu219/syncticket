import { useState } from 'react'
import type { TicketResponse, TicketStatus } from '../api/types'
import { ApiError } from '../api/ApiError'
import { transitionTicket } from '../api/tickets'
import {
  transitionButtonLabel,
  transitionNeedsConfirm,
  transitionNeedsNote,
} from '../utils/transitionLabels'
import { ErrorBanner } from './ErrorBanner'
import { FieldError } from './FieldError'

interface StatusActionsProps {
  ticket: TicketResponse
  onUpdated: (ticket: TicketResponse) => void
  onReload: () => void
}

export function StatusActions({ ticket, onUpdated, onReload }: StatusActionsProps) {
  const [error, setError] = useState<ApiError | null>(null)
  const [pending, setPending] = useState<TicketStatus | null>(null)
  const [note, setNote] = useState('')
  const [noteError, setNoteError] = useState<string>()
  const [submitting, setSubmitting] = useState(false)

  async function runTransition(target: TicketStatus, resolutionNote?: string) {
    setSubmitting(true)
    setError(null)
    setNoteError(undefined)
    try {
      const body = {
        targetStatus: target,
        version: ticket.version,
        ...(resolutionNote ? { note: resolutionNote } : {}),
      }
      const updated = await transitionTicket(ticket.id, body)
      onUpdated(updated)
      setPending(null)
      setNote('')
    } catch (e) {
      const err = e instanceof ApiError ? e : ApiError.network()
      setError(err)
      if (err.code === 'TICKET_NOT_EDITABLE' || err.code === 'INVALID_STATUS_TRANSITION') {
        onReload()
      }
    } finally {
      setSubmitting(false)
    }
  }

  function onActionClick(target: TicketStatus) {
    setError(null)
    if (transitionNeedsNote(target)) {
      setPending(target)
      setNote('')
      return
    }
    if (transitionNeedsConfirm(target)) {
      if (!window.confirm('Close this ticket?')) return
    }
    void runTransition(target)
  }

  function confirmPending() {
    if (!pending) return
    if (transitionNeedsConfirm(pending)) {
      const msg =
        pending === 'CANCELLED'
          ? 'Cancel this ticket? This cannot be undone.'
          : 'Close this ticket?'
      if (!window.confirm(msg)) return
    }
    const trimmed = note.trim()
    if (transitionNeedsNote(pending) && trimmed.length === 0) {
      setNoteError('Resolution note is required.')
      return
    }
    void runTransition(pending, trimmed || undefined)
  }

  if (ticket.allowedTransitions.length === 0) return null

  return (
    <section className="panel" aria-label="Status actions">
      <h2>Status actions</h2>
      {error && (
        <ErrorBanner
          message={error.detail}
          onRetry={error.code === 'NETWORK_ERROR' ? () => setError(null) : undefined}
          onReload={error.code === 'CONCURRENT_MODIFICATION' ? onReload : undefined}
        />
      )}
      <div className="status-actions">
        {ticket.allowedTransitions.map((target) => (
          <button
            key={target}
            type="button"
            className="btn btn-secondary"
            disabled={submitting}
            onClick={() => onActionClick(target)}
          >
            {transitionButtonLabel(ticket.status, target)}
          </button>
        ))}
      </div>
      {pending && (
        <div className="transition-note-panel">
          <p>
            {transitionButtonLabel(ticket.status, pending)} — add a resolution note
          </p>
          <label htmlFor="resolution-note">Resolution note *</label>
          <textarea
            id="resolution-note"
            rows={3}
            value={note}
            onChange={(e) => {
              setNote(e.target.value)
              setNoteError(undefined)
            }}
            aria-describedby="resolution-note-error"
          />
          <FieldError id="resolution-note-error" message={noteError} />
          <div className="form-actions">
            <button
              type="button"
              className="btn btn-secondary"
              onClick={() => setPending(null)}
            >
              Cancel
            </button>
            <button
              type="button"
              className="btn btn-primary"
              disabled={submitting}
              onClick={confirmPending}
            >
              {submitting ? 'Saving…' : 'Confirm'}
            </button>
          </div>
        </div>
      )}
    </section>
  )
}
