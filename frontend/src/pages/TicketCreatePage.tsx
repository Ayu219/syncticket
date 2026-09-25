import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { ApiError } from '../api/ApiError'
import { createTicket } from '../api/tickets'
import { ErrorBanner } from '../components/ErrorBanner'
import { TicketForm, type TicketFormValues } from '../components/TicketForm'
import { useToast } from '../components/Toast'

export function TicketCreatePage() {
  const navigate = useNavigate()
  const { showToast } = useToast()
  const [submitting, setSubmitting] = useState(false)
  const [bannerError, setBannerError] = useState<string | null>(null)
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})

  async function handleSubmit(values: TicketFormValues) {
    setSubmitting(true)
    setBannerError(null)
    setFieldErrors({})
    try {
      const created = await createTicket({
        title: values.title,
        description: values.description,
        priority: values.priority,
        assignee: values.assignee.trim() || undefined,
      })
      showToast(`Ticket #${created.id} created`)
      navigate(`/tickets/${created.id}`)
    } catch (e) {
      const err = e instanceof ApiError ? e : ApiError.network()
      if (err.fieldErrors.length) {
        const map: Record<string, string> = {}
        err.fieldErrors.forEach((fe) => {
          map[fe.field] = fe.message
        })
        setFieldErrors(map)
      } else {
        setBannerError(err.detail)
      }
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="page narrow">
      <header className="page-header">
        <h1>New ticket</h1>
        <Link to="/tickets" className="btn btn-secondary">Cancel</Link>
      </header>
      {bannerError && <ErrorBanner message={bannerError} />}
      <TicketForm
        initial={{
          title: '',
          description: '',
          priority: 'MEDIUM',
          assignee: '',
        }}
        submitLabel="Create ticket"
        fieldErrors={fieldErrors}
        submitting={submitting}
        onSubmit={handleSubmit}
        onFieldChange={(field) =>
          setFieldErrors((f) => {
            const next = { ...f }
            delete next[field]
            return next
          })
        }
      />
    </div>
  )
}
