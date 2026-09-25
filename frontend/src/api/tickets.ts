import { apiRequest } from './client'
import type {
  CommentResponse,
  CreateTicketRequest,
  PageResponse,
  StatusHistoryResponse,
  TicketResponse,
  TicketSummaryResponse,
  TicketStatus,
  TransitionRequest,
  UpdateTicketRequest,
} from './types'

export interface ListTicketsParams {
  q?: string
  status?: TicketStatus
  page: number
  size?: number
  sort?: string
}

export function listTickets(params: ListTicketsParams) {
  const search = new URLSearchParams()
  if (params.q) search.set('q', params.q)
  if (params.status) search.set('status', params.status)
  search.set('page', String(params.page))
  search.set('size', String(params.size ?? 20))
  search.set('sort', params.sort ?? 'createdAt,desc')
  const qs = search.toString()
  return apiRequest<PageResponse<TicketSummaryResponse>>(`/tickets?${qs}`)
}

export function getTicket(id: number) {
  return apiRequest<TicketResponse>(`/tickets/${id}`)
}

export function createTicket(body: CreateTicketRequest) {
  return apiRequest<TicketResponse>('/tickets', {
    method: 'POST',
    body: JSON.stringify(body),
  })
}

export function updateTicket(id: number, body: UpdateTicketRequest) {
  return apiRequest<TicketResponse>(`/tickets/${id}`, {
    method: 'PATCH',
    body: JSON.stringify(body),
  })
}

export function transitionTicket(id: number, body: TransitionRequest) {
  return apiRequest<TicketResponse>(`/tickets/${id}/transitions`, {
    method: 'POST',
    body: JSON.stringify(body),
  })
}

export function listComments(ticketId: number) {
  return apiRequest<CommentResponse[]>(`/tickets/${ticketId}/comments`)
}

export function addComment(
  ticketId: number,
  body: { author: string; body: string },
) {
  return apiRequest<CommentResponse>(`/tickets/${ticketId}/comments`, {
    method: 'POST',
    body: JSON.stringify(body),
  })
}

export function updateComment(
  ticketId: number,
  commentId: number,
  body: { body: string },
) {
  return apiRequest<CommentResponse>(
    `/tickets/${ticketId}/comments/${commentId}`,
    {
      method: 'PATCH',
      body: JSON.stringify(body),
    },
  )
}

export function deleteComment(ticketId: number, commentId: number) {
  return apiRequest<void>(`/tickets/${ticketId}/comments/${commentId}`, {
    method: 'DELETE',
  })
}

export function listStatusHistory(ticketId: number) {
  return apiRequest<StatusHistoryResponse[]>(`/tickets/${ticketId}/history`)
}
