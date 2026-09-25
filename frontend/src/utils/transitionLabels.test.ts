import { describe, expect, it } from 'vitest'
import {
  transitionButtonLabel,
  transitionNeedsConfirm,
  transitionNeedsNote,
} from './transitionLabels'

describe('transitionLabels', () => {
  it('labels reopen vs start progress', () => {
    expect(transitionButtonLabel('OPEN', 'IN_PROGRESS')).toBe('Start progress')
    expect(transitionButtonLabel('RESOLVED', 'IN_PROGRESS')).toBe('Reopen')
  })

  it('flags note and confirm targets', () => {
    expect(transitionNeedsNote('RESOLVED')).toBe(true)
    expect(transitionNeedsNote('CANCELLED')).toBe(true)
    expect(transitionNeedsNote('IN_PROGRESS')).toBe(false)
    expect(transitionNeedsConfirm('CLOSED')).toBe(true)
    expect(transitionNeedsConfirm('OPEN')).toBe(false)
  })
})
