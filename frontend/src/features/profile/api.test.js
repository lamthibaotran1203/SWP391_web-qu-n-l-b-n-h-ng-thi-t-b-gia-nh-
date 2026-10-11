import { beforeEach, expect, it, vi } from 'vitest'
import { apiRequest } from '../../api/http'
import { updatePersonalProfile } from './api'

vi.mock('../../api/http', () => ({ apiRequest: vi.fn() }))
beforeEach(() => vi.clearAllMocks())

it('sends only editable contact fields and clears a blank phone with null', async () => {
  await updatePersonalProfile({ fullName: 'Nguyễn An', phone: ' ', email: 'other@example.invalid', role: 'MANAGER' })
  expect(apiRequest).toHaveBeenCalledWith('/api/v1/account/profile', expect.objectContaining({ method: 'PUT' }))
  expect(JSON.parse(apiRequest.mock.calls[0][1].body)).toEqual({ fullName: 'Nguyễn An', phone: null })
})
