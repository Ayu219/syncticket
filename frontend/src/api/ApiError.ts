export interface FieldError {
  field: string
  message: string
}

export class ApiError extends Error {
  readonly status: number
  readonly code: string
  readonly detail: string
  readonly fieldErrors: FieldError[]

  constructor(
    status: number,
    code: string,
    detail: string,
    fieldErrors: FieldError[] = [],
  ) {
    super(detail)
    this.name = 'ApiError'
    this.status = status
    this.code = code
    this.detail = detail
    this.fieldErrors = fieldErrors
  }

  static network(): ApiError {
    return new ApiError(
      0,
      'NETWORK_ERROR',
      'Cannot reach the server. Check your connection and try again.',
    )
  }

  fieldMessage(field: string): string | undefined {
    return this.fieldErrors.find((e) => e.field === field)?.message
  }
}
