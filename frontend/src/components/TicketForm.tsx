import { useId, useState } from 'react'
import type { TicketPriority } from '../api/types'
import { TICKET_PRIORITIES } from '../constants'
import { FieldError } from './FieldError'

export interface TicketFormValues {
  title: string
  description: string
  priority: TicketPriority
  assignee: string
}

interface TicketFormProps {
  initial: TicketFormValues
  submitLabel: string
  fieldErrors: Record<string, string>
  submitting: boolean
  onSubmit: (values: TicketFormValues) => void
  onCancel?: () => void
  onFieldChange?: (field: string) => void
}

export function TicketForm({
  initial,
  submitLabel,
  fieldErrors,
  submitting,
  onSubmit,
  onCancel,
  onFieldChange,
}: TicketFormProps) {
  const [values, setValues] = useState(initial)
  const titleId = useId()
  const descriptionId = useId()
  const priorityId = useId()
  const assigneeId = useId()

  function update<K extends keyof TicketFormValues>(key: K, val: TicketFormValues[K]) {
    setValues((v) => ({ ...v, [key]: val }))
    onFieldChange?.(key)
  }

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    onSubmit(values)
  }

  return (
    <form className="ticket-form" onSubmit={handleSubmit} noValidate>
      <div className="field">
        <label htmlFor={titleId}>Title *</label>
        <input
          id={titleId}
          value={values.title}
          onChange={(e) => update('title', e.target.value)}
          aria-describedby={fieldErrors.title ? `${titleId}-error` : undefined}
          aria-invalid={!!fieldErrors.title}
        />
        <FieldError id={`${titleId}-error`} message={fieldErrors.title} />
      </div>
      <div className="field">
        <label htmlFor={descriptionId}>Description *</label>
        <textarea
          id={descriptionId}
          rows={5}
          value={values.description}
          onChange={(e) => update('description', e.target.value)}
          aria-describedby={fieldErrors.description ? `${descriptionId}-error` : undefined}
          aria-invalid={!!fieldErrors.description}
        />
        <FieldError id={`${descriptionId}-error`} message={fieldErrors.description} />
      </div>
      <div className="field">
        <label htmlFor={priorityId}>Priority *</label>
        <select
          id={priorityId}
          value={values.priority}
          onChange={(e) => update('priority', e.target.value as TicketPriority)}
        >
          {TICKET_PRIORITIES.map((p) => (
            <option key={p} value={p}>{p}</option>
          ))}
        </select>
        <FieldError id={`${priorityId}-error`} message={fieldErrors.priority} />
      </div>
      <div className="field">
        <label htmlFor={assigneeId}>Assignee</label>
        <input
          id={assigneeId}
          value={values.assignee}
          onChange={(e) => update('assignee', e.target.value)}
          aria-describedby={fieldErrors.assignee ? `${assigneeId}-error` : undefined}
        />
        <FieldError id={`${assigneeId}-error`} message={fieldErrors.assignee} />
      </div>
      <div className="form-actions">
        {onCancel && (
          <button type="button" className="btn btn-secondary" onClick={onCancel}>
            Cancel
          </button>
        )}
        <button type="submit" className="btn btn-primary" disabled={submitting}>
          {submitting ? 'Saving…' : submitLabel}
        </button>
      </div>
    </form>
  )
}
