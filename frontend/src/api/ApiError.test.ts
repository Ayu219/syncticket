import { describe, expect, it } from 'vitest'
import { ApiError } from './ApiError'

describe('ApiError', () => {
  it('finds field message', () => {
    const error = new ApiError(400, 'VALIDATION_FAILED', 'Invalid', [
      { field: 'title', message: 'Too short' },
    ])
    expect(error.fieldMessage('title')).toBe('Too short')
    expect(error.fieldMessage('missing')).toBeUndefined()
  })

  it('builds network error', () => {
    const error = ApiError.network()
    expect(error.status).toBe(0)
    expect(error.code).toBe('NETWORK_ERROR')
  })
})
