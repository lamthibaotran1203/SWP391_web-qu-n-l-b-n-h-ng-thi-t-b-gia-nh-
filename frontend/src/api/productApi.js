import { apiRequest } from './http'

export function getProducts({ search = '', brand = '', category = '', minPrice = '', maxPrice = '', sort = 'newest', page = 0, size = 12 } = {}) {
  const params = new URLSearchParams({ page, size })
  if (search.trim()) params.set('search', search.trim())
  if (brand.trim()) params.set('brand', brand.trim())
  if (category.trim()) params.set('category', category.trim())
  if (String(minPrice).trim()) params.set('minPrice', String(minPrice).trim())
  if (String(maxPrice).trim()) params.set('maxPrice', String(maxPrice).trim())
  if (sort) params.set('sort', sort)
  return apiRequest(`/api/v1/products?${params.toString()}`)
}

export function getProductById(id) {
  return apiRequest(`/api/v1/products/${id}`)
}

export function createProduct(data) {
  return apiRequest('/api/v1/products', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data),
  })
}

export function updateProduct(id, data) {
  return apiRequest(`/api/v1/products/${id}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data),
  })
}

export function deleteProduct(id) {
  return apiRequest(`/api/v1/products/${id}`, { method: 'DELETE' })
}
