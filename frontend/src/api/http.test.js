import { beforeEach, describe, expect, it, vi } from 'vitest'
import {
  apiRequest,
  changeOwnPassword,
  confirmSalesOrder,
  getSalesOrders,
  refreshCsrfToken,
  updateCurrentUser,
} from './http'

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

  it('sends supported order search filters as query parameters', async () => {
    fetch.mockResolvedValueOnce(jsonResponse({
      data: { content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 },
    }))

    await getSalesOrders({
      q: 'HG-2026',
      channel: 'ONLINE',
      status: '',
      paymentStatus: 'PAID',
      from: '',
      to: '',
      page: 0,
      size: 20,
    })

    const requestUrl = new URL(fetch.mock.calls[0][0], 'http://localhost')
    expect(requestUrl.pathname).toBe('/api/v1/staff/orders')
    expect(requestUrl.searchParams.get('q')).toBe('HG-2026')
    expect(requestUrl.searchParams.get('channel')).toBe('ONLINE')
    expect(requestUrl.searchParams.get('paymentStatus')).toBe('PAID')
    expect(requestUrl.searchParams.get('page')).toBe('0')
    expect(requestUrl.searchParams.get('size')).toBe('20')
    expect(requestUrl.searchParams.has('status')).toBe(false)
  })

  it('confirms an order with a CSRF-protected POST and no request body', async () => {
    fetch
      .mockResolvedValueOnce(jsonResponse({
        data: { headerName: 'X-XSRF-TOKEN', token: 'confirm-token' },
      }))
      .mockResolvedValueOnce(jsonResponse({
        data: { id: 42, status: 'CONFIRMED', paymentStatus: 'PENDING' },
      }))

    await refreshCsrfToken()
    const result = await confirmSalesOrder(42)

    const [url, options] = fetch.mock.calls[1]
    expect(url).toBe('/api/v1/staff/orders/42/confirm')
    expect(options.method).toBe('POST')
    expect(options.body).toBeUndefined()
    expect(options.headers.get('X-XSRF-TOKEN')).toBe('confirm-token')
    expect(result).toEqual({ id: 42, status: 'CONFIRMED', paymentStatus: 'PENDING' })
  })

  it('updates the current user display name with CSRF protection', async () => {
    fetch
      .mockResolvedValueOnce(jsonResponse({
        data: {
          headerName: 'X-XSRF-TOKEN',
          parameterName: '_csrf',
          token: 'profile-token',
        },
      }))
      .mockResolvedValueOnce(jsonResponse({
        data: { authenticated: true, fullName: 'Bảo Trân', role: 'MANAGER' },
      }))

    await refreshCsrfToken()
    const result = await updateCurrentUser('Bảo Trân')

    const [url, options] = fetch.mock.calls[1]
    expect(url).toBe('/api/v1/auth/me')
    expect(options.method).toBe('PATCH')
    expect(options.headers.get('X-XSRF-TOKEN')).toBe('profile-token')
    expect(JSON.parse(options.body)).toEqual({ fullName: 'Bảo Trân' })
    expect(result.fullName).toBe('Bảo Trân')
  })
})
