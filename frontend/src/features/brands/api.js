
import { apiRequest } from '../../api/http'

const BASE_PATH = '/api/v1/manager/brands'

// UC-12.2: View Brands (List & Search & Filter & Pagination)
export function getBrands({ keyword = '', status = '', page = 0, size = 10 } = {}) {
  const params = new URLSearchParams()

  if (keyword && keyword.trim()) {
    params.append('keyword', keyword.trim())
  }

  if (status && status.trim()) {
    params.append('status', status.trim())
  }

  params.append('page', String(page))
  params.append('size', String(size))

  return apiRequest(`${BASE_PATH}?${params.toString()}`)
}

// UC-12.2: View Brand Detail
export function getBrandById(id) {
  return apiRequest(`${BASE_PATH}/${encodeURIComponent(id)}`)
}

// UC-12.1: Create Brand
export function createBrand(payload) {
  return apiRequest(BASE_PATH, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  })
}

// UC-12.3: Update Brand
export function updateBrand(id, payload) {
  return apiRequest(`${BASE_PATH}/${encodeURIComponent(id)}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  })
}

// UC-12.4: Deactivate Brand
export function deactivateBrand(id) {
  return apiRequest(`${BASE_PATH}/${encodeURIComponent(id)}`, {
    method: 'DELETE',
  })
}

// UC-12.4 Extension: Reactivate Brand
// Khoi phuc thuong hieu da ngung hoat dong
export function reactivateBrand(id) {
  return apiRequest(`${BASE_PATH}/${encodeURIComponent(id)}/reactivate`, {
    method: 'PATCH',
  })
}
