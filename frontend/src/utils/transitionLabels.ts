import type { TicketStatus } from '../api/types'

export function transitionButtonLabel(
  from: TicketStatus,
  to: TicketStatus,
): string {
  if (to === 'IN_PROGRESS') {
    return from === 'RESOLVED' ? 'Reopen' : 'Start progress'
  }
  if (to === 'RESOLVED') return 'Mark resolved'
  if (to === 'CLOSED') return 'Close ticket'
  if (to === 'CANCELLED') return 'Cancel ticket'
  return to.replace('_', ' ')
}

export function transitionNeedsNote(target: TicketStatus): boolean {
  return target === 'RESOLVED' || target === 'CANCELLED'
}

export function transitionNeedsConfirm(target: TicketStatus): boolean {
  return target === 'CLOSED' || target === 'CANCELLED'
}
