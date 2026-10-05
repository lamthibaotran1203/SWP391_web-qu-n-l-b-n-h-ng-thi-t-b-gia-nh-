import { describe, expect, it } from 'vitest'
import { formatVnd } from './api'
describe('whole VND display', () => {
  it('preserves prices above the JavaScript safe integer range', () => {
    expect(formatVnd('999999999999999999')).toContain('999.999.999.999.999.999')
    expect(() => formatVnd('-1')).toThrow(TypeError)
  })
})
