let csrfState = null

const unsafeMethods = new Set(['POST', 'PUT', 'PATCH', 'DELETE'])

export class ApiError extends Error {
  constructor(problem, status) {
    super(problem?.detail || 'Không thể kết nối đến máy chủ.')
    this.name = 'ApiError'
    this.status = status
    this.code = problem?.code || 'NETWORK_ERROR'
    this.fieldErrors = problem?.fieldErrors || {}
    this.traceId = problem?.traceId
  }
}

async function parseBody(response) {
  const contentType = response.headers.get('content-type') || ''

  if (!contentType.includes('json')) {
    return null
  }

  return response.json()
}

export async function refreshCsrfToken() {
  const response = await fetch('/api/v1/auth/csrf', {
    credentials: 'same-origin',
  })

  const body = await parseBody(response)

  if (!response.ok || !body?.data?.token) {
    throw new ApiError(body, response.status)
  }

  csrfState = body.data
  return csrfState
}

export async function apiRequest(
  path,
  options = {},
  allowCsrfRetry = true,
) {
  const method = (options.method || 'GET').toUpperCase()
  const headers = new Headers(options.headers)

  if (unsafeMethods.has(method)) {
    if (!csrfState) {
      await refreshCsrfToken()
    }

    headers.set(csrfState.headerName, csrfState.token)
  }

  const response = await fetch(path, {
    ...options,
    method,
    headers,
    credentials: 'same-origin',
  })

  const body = await parseBody(response)

  if (!response.ok) {
    if (
      response.status === 403
      && body?.code === 'CSRF_INVALID'
      && allowCsrfRetry
    ) {
      await refreshCsrfToken()
      return apiRequest(path, options, false)
    }

    throw new ApiError(body, response.status)
  }

  return body?.data
}

export function getCurrentUser() {
  return apiRequest('/api/v1/auth/me')
}

export function updateCurrentUser(fullName) {
  return apiRequest('/api/v1/auth/me', {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ fullName }),
  })
}

export function login(email, password) {
  return apiRequest('/api/v1/auth/login', {
    method: 'POST',
    body: new URLSearchParams({
      email,
      password,
    }),
  })
}

export function logout() {
  return apiRequest('/api/v1/auth/logout', {
    method: 'POST',
  })
}

export function getSalesOrders(filters) {
  const params = new URLSearchParams()
  for (const [key, value] of Object.entries(filters)) {
    if (value !== '' && value !== null && value !== undefined) {
      params.set(key, value)
    }
  }
  return apiRequest(`/api/v1/staff/orders?${params.toString()}`)
}

export function getSalesOrderDetails(id) {
  return apiRequest(`/api/v1/staff/orders/${encodeURIComponent(id)}`)
}

export function confirmSalesOrder(id) {
  return apiRequest(`/api/v1/staff/orders/${encodeURIComponent(id)}/confirm`, {
    method: 'POST',
  })
}

export function register(email, password, fullName) {
  return apiRequest('/api/v1/auth/register', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({
      email,
      password,
      fullName,
    }),
  })
}

export function resendVerification(email) {
  return apiRequest('/api/v1/auth/resend-verification', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({
      email,
    }),
  })
}

export function verifyEmail(token, options = {}) {
  const query = new URLSearchParams({ token })

  return apiRequest(
    `/api/v1/auth/verify-email?${query.toString()}`,
    options,
  )
}

export function changeOwnPassword(currentPassword, newPassword, confirmation) {
  return apiRequest('/api/v1/auth/password', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ currentPassword, newPassword, confirmation }),
  }, false) // Never silently resubmit a credential change after a CSRF failure.
}
