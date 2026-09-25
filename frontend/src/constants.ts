import type { TicketPriority, TicketStatus } from './api/types'

export const TICKET_STATUSES: TicketStatus[] = [
  'OPEN',
  'IN_PROGRESS',
  'RESOLVED',
  'CLOSED',
  'CANCELLED',
]

export const TICKET_PRIORITIES: TicketPriority[] = [
  'LOW',
  'MEDIUM',
  'HIGH',
  'CRITICAL',
]

export const COMMENT_AUTHOR_KEY = 'syncticket.commentAuthor'

export function isTerminalStatus(status: TicketStatus): boolean {
  return status === 'CLOSED' || status === 'CANCELLED'
}
