import type { TicketStatus } from '../api/types'

const STATUS_CLASS: Record<TicketStatus, string> = {
  OPEN: 'status-open',
  IN_PROGRESS: 'status-progress',
  RESOLVED: 'status-resolved',
  CLOSED: 'status-closed',
  CANCELLED: 'status-cancelled',
}

export function StatusBadge({ status }: { status: TicketStatus }) {
  return (
    <span className={`status-badge ${STATUS_CLASS[status]}`}>{status.replace('_', ' ')}</span>
  )
}
