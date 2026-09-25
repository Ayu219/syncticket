export type TicketPriority = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL'
export type TicketStatus = 'OPEN' | 'IN_PROGRESS' | 'RESOLVED' | 'CLOSED' | 'CANCELLED'

export interface TicketResponse {
  id: number
  title: string
  description: string
  priority: TicketPriority
  status: TicketStatus
  assignee: string | null
  allowedTransitions: TicketStatus[]
  commentCount: number
  createdAt: string
  updatedAt: string
  version: number
}

export interface TicketSummaryResponse {
  id: number
  title: string
  priority: TicketPriority
  status: TicketStatus
  assignee: string | null
  commentCount: number
  createdAt: string
  updatedAt: string
  version: number
}

export interface PageResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export interface CommentResponse {
  id: number
  ticketId: number
  author: string
  body: string
  createdAt: string
}

export interface StatusHistoryResponse {
  fromStatus: TicketStatus | null
  toStatus: TicketStatus
  changedAt: string
  note: string | null
}

export interface CreateTicketRequest {
  title: string
  description: string
  priority: TicketPriority
  assignee?: string
}

export interface UpdateTicketRequest {
  title?: string
  description?: string
  priority?: TicketPriority
  assignee?: string | null
  version: number
}

export interface TransitionRequest {
  targetStatus: TicketStatus
  version: number
  note?: string
}
