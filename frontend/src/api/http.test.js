import { beforeEach, describe, expect, it, vi } from 'vitest'
import { apiRequest, changeOwnPassword, refreshCsrfToken } from './http'

function jsonResponse(body, status = 200) {
  return {
    ok: status >= 200 && status < 300,
    status,
    headers: {
      get: () => 'application/json',
    },
    json: async () => body,
  }
}

describe('API client CSRF flow', () => {
  beforeEach(() => {
    vi.stubGlobal('fetch', vi.fn())
  })

  it('uses the server-provided CSRF header for an unsafe request', async () => {
    fetch
      .mockResolvedValueOnce(jsonResponse({
        data: {
          headerName: 'X-XSRF-TOKEN',
          parameterName: '_csrf',
          token: 'server-token',
        },
      }))
      .mockResolvedValueOnce(jsonResponse({ data: { saved: true } }))

    await refreshCsrfToken()
    const result = await apiRequest('/api/v1/example', { method: 'POST' })

    const requestOptions = fetch.mock.calls[1][1]
    expect(requestOptions.headers.get('X-XSRF-TOKEN')).toBe('server-token')
    expect(requestOptions.credentials).toBe('same-origin')
    expect(result).toEqual({ saved: true })
  })

  it('does not automatically replay a password change when CSRF expires', async () => {
    fetch.mockResolvedValueOnce(jsonResponse({ data: { headerName: 'X-XSRF-TOKEN', token: 'fresh-token' } }))
      .mockResolvedValueOnce(jsonResponse({ code: 'CSRF_INVALID', detail: 'Phiên đã hết hạn.' }, 403))
    await refreshCsrfToken()
    await expect(changeOwnPassword('old password', 'new password', 'new password')).rejects.toMatchObject({ status: 403 })
    expect(fetch).toHaveBeenCalledTimes(2)
    const [path, options] = fetch.mock.calls[1]
    expect(path).toBe('/api/v1/auth/password')
    expect(JSON.parse(options.body)).toEqual({ currentPassword: 'old password', newPassword: 'new password', confirmation: 'new password' })
    expect(options.headers.get('X-XSRF-TOKEN')).toBe('fresh-token')
  })
})
