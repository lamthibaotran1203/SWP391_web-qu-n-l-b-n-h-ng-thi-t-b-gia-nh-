import { apiRequest } from '../../api/http'

export function getPosCatalog({ search = '', page = 0, size = 12 }, options = {}) {
  const query = new URLSearchParams({ page: String(page), size: String(size) })
  if (search.trim()) query.set('search', search.trim())
  return apiRequest(`/api/v1/catalog/skus?${query}`, options)
}

export function processPosTransaction(items, paymentMethod, idempotencyKey) {
  return apiRequest('/api/v1/staff/pos/transactions', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Idempotency-Key': idempotencyKey,
    },
    body: JSON.stringify({ items, paymentMethod }),
  })
}
