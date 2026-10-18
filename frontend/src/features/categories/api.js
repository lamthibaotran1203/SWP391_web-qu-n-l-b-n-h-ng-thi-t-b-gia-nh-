import { apiRequest } from '../../api/http'

const BASE_PATH = '/api/v1/admin/categories'

export function getCategories({ keyword = '', status = '', page = 0, size = 10 } = {}) {
  const query = new URLSearchParams({ page: String(page), size: String(size) })
  if (keyword.trim()) query.set('keyword', keyword.trim())
  if (status) query.set('status', status)
  return apiRequest(`${BASE_PATH}?${query.toString()}`)
}

export function getCategory(id) {
  return apiRequest(`${BASE_PATH}/${encodeURIComponent(id)}`)
}

export function getCategoryOptions(excludeId) {
  const suffix = excludeId ? `?excludeId=${encodeURIComponent(excludeId)}` : ''
  return apiRequest(`${BASE_PATH}/options${suffix}`)
}

export function createCategory(payload) {
  return apiRequest(BASE_PATH, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  })
}

export function updateCategory(id, payload) {
  return apiRequest(`${BASE_PATH}/${encodeURIComponent(id)}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  })
}

export function deactivateCategory(id) {
  return apiRequest(`${BASE_PATH}/${encodeURIComponent(id)}`, { method: 'DELETE' })
}
