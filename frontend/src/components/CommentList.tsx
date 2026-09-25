import { useState } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { ApiError } from '../api/ApiError'
import {
  addComment,
  deleteComment,
  listComments,
  updateComment,
} from '../api/tickets'
import type { TicketResponse } from '../api/types'
import { COMMENT_AUTHOR_KEY, isTerminalStatus } from '../constants'
import { formatDateTime } from '../utils/dates'
import { ErrorBanner } from './ErrorBanner'
import { FieldError } from './FieldError'

interface CommentListProps {
  ticket: TicketResponse
  onTicketRefetch: () => void
}

export function CommentList({ ticket, onTicketRefetch }: CommentListProps) {
  const editable = !isTerminalStatus(ticket.status)
  const queryClient = useQueryClient()
  const [author, setAuthor] = useState(
    () => sessionStorage.getItem(COMMENT_AUTHOR_KEY) ?? '',
  )
  const [body, setBody] = useState('')
  const [formError, setFormError] = useState<ApiError | null>(null)
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})
  const [editingId, setEditingId] = useState<number | null>(null)
  const [editBody, setEditBody] = useState('')
  const [submitting, setSubmitting] = useState(false)

  const { data: comments, isLoading, isError, refetch } = useQuery({
    queryKey: ['comments', ticket.id],
    queryFn: () => listComments(ticket.id),
  })

  async function handleAdd(e: React.FormEvent) {
    e.preventDefault()
    setSubmitting(true)
    setFormError(null)
    setFieldErrors({})
    try {
      await addComment(ticket.id, { author: author.trim(), body: body.trim() })
      sessionStorage.setItem(COMMENT_AUTHOR_KEY, author.trim())
      setBody('')
      await queryClient.invalidateQueries({ queryKey: ['comments', ticket.id] })
      onTicketRefetch()
    } catch (err) {
      const apiErr = err instanceof ApiError ? err : ApiError.network()
      setFormError(apiErr)
      const map: Record<string, string> = {}
      apiErr.fieldErrors.forEach((fe) => {
        map[fe.field] = fe.message
      })
      setFieldErrors(map)
      if (apiErr.code === 'TICKET_NOT_EDITABLE') onTicketRefetch()
    } finally {
      setSubmitting(false)
    }
  }

  async function saveEdit(commentId: number) {
    setSubmitting(true)
    try {
      await updateComment(ticket.id, commentId, { body: editBody.trim() })
      setEditingId(null)
      await queryClient.invalidateQueries({ queryKey: ['comments', ticket.id] })
    } catch (err) {
      const apiErr = err instanceof ApiError ? err : ApiError.network()
      alert(apiErr.detail)
      if (apiErr.code === 'TICKET_NOT_EDITABLE') onTicketRefetch()
    } finally {
      setSubmitting(false)
    }
  }

  async function handleDelete(commentId: number) {
    if (!window.confirm('Delete this comment?')) return
    try {
      await deleteComment(ticket.id, commentId)
      await queryClient.invalidateQueries({ queryKey: ['comments', ticket.id] })
      onTicketRefetch()
    } catch (err) {
      const apiErr = err instanceof ApiError ? err : ApiError.network()
      alert(apiErr.detail)
    }
  }

  return (
    <section className="panel" aria-label="Comments">
      <h2>Comments ({comments?.length ?? ticket.commentCount})</h2>
      {isLoading && <p className="muted">Loading comments…</p>}
      {isError && (
        <ErrorBanner message="Could not load comments." onRetry={() => void refetch()} />
      )}
      <ul className="comment-list">
        {comments?.map((c) => (
          <li key={c.id}>
            <div className="comment-meta">
              <strong>{c.author}</strong>
              <span className="muted"> · {formatDateTime(c.createdAt)}</span>
            </div>
            {editingId === c.id ? (
              <>
                <textarea
                  rows={3}
                  value={editBody}
                  onChange={(e) => setEditBody(e.target.value)}
                />
                <div className="form-actions">
                  <button type="button" className="btn btn-secondary" onClick={() => setEditingId(null)}>
                    Cancel
                  </button>
                  <button
                    type="button"
                    className="btn btn-primary"
                    disabled={submitting}
                    onClick={() => void saveEdit(c.id)}
                  >
                    Save
                  </button>
                </div>
              </>
            ) : (
              <p>{c.body}</p>
            )}
            {editable && editingId !== c.id && (
              <div className="comment-actions">
                <button
                  type="button"
                  className="btn-link"
                  onClick={() => {
                    setEditingId(c.id)
                    setEditBody(c.body)
                  }}
                >
                  Edit
                </button>
                <button
                  type="button"
                  className="btn-link"
                  onClick={() => void handleDelete(c.id)}
                >
                  Delete
                </button>
              </div>
            )}
          </li>
        ))}
      </ul>
      {editable ? (
        <form className="comment-form" onSubmit={(e) => void handleAdd(e)}>
          {formError && !Object.keys(fieldErrors).length && (
            <ErrorBanner message={formError.detail} />
          )}
          <div className="field">
            <label htmlFor="comment-author">Your name</label>
            <input
              id="comment-author"
              value={author}
              onChange={(e) => {
                setAuthor(e.target.value)
                setFieldErrors((f) => ({ ...f, author: '' }))
              }}
            />
            <FieldError id="author-error" message={fieldErrors.author} />
          </div>
          <div className="field">
            <label htmlFor="comment-body">Comment</label>
            <textarea
              id="comment-body"
              rows={3}
              value={body}
              onChange={(e) => {
                setBody(e.target.value)
                setFieldErrors((f) => ({ ...f, body: '' }))
              }}
            />
            <FieldError id="body-error" message={fieldErrors.body} />
          </div>
          <button type="submit" className="btn btn-primary" disabled={submitting}>
            {submitting ? 'Adding…' : 'Add'}
          </button>
        </form>
      ) : (
        <p className="muted closed-message">This ticket is closed and can no longer be changed.</p>
      )}
    </section>
  )
}
