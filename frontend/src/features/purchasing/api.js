import { apiRequest } from '../../api/http'

// UC-27: Supplier APIs
export function getSuppliers({ keyword = '', status = '', page = 0, size = 10 } = {}) {
  const params = new URLSearchParams()
  if (keyword) params.append('keyword', keyword)
  if (status) params.append('status', status)
  params.append('page', String(page))
  params.append('size', String(size))
  return apiRequest(`/api/v1/purchasing/suppliers?${params.toString()}`)
}

export function getActiveSuppliers() {
  return apiRequest('/api/v1/purchasing/suppliers/active')
}

export function getSupplier(id) {
  return apiRequest(`/api/v1/purchasing/suppliers/${encodeURIComponent(id)}`)
}

export function createSupplier(payload) {
  return apiRequest('/api/v1/purchasing/suppliers', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  })
}

export function updateSupplier(id, payload) {
  return apiRequest(`/api/v1/purchasing/suppliers/${encodeURIComponent(id)}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  })
}

export function deleteSupplier(id) {
  return apiRequest(`/api/v1/purchasing/suppliers/${encodeURIComponent(id)}`, {
    method: 'DELETE',
  })
}

// UC-28: Purchase Order APIs
export function createPurchaseOrder(payload) {
  return apiRequest('/api/v1/purchasing/purchase-orders', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  })
}

export function getPurchaseOrders({ keyword = '', supplierId = '', status = '', page = 0, size = 10 } = {}) {
  const params = new URLSearchParams()
  if (keyword) params.append('keyword', keyword)
  if (supplierId) params.append('supplierId', String(supplierId))
  if (status) params.append('status', status)
  params.append('page', String(page))
  params.append('size', String(size))
  return apiRequest(`/api/v1/purchasing/purchase-orders?${params.toString()}`)
}

export function getPurchaseOrder(id) {
  return apiRequest(`/api/v1/purchasing/purchase-orders/${encodeURIComponent(id)}`)
}

export function getAvailableSkus() {
  return apiRequest('/api/v1/catalog/skus?size=100')
}

export function formatVnd(value) {
  if (value === null || value === undefined) return '0 ₫'
  const numeric = typeof value === 'string' ? Number(value) : value
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(numeric)
}
