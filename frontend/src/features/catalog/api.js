import { apiRequest } from '../../api/http'

// Shared read contract only; the UC owners implement their own screens/workflows.
export function getCatalogSkus({ page = 0, size = 20 } = {}) {
  const query = new URLSearchParams({ page: String(page), size: String(size) })
  return apiRequest(`/api/v1/catalog/skus?${query}`)
}
export function getCatalogSku(skuId) {
  return apiRequest(`/api/v1/catalog/skus/${encodeURIComponent(skuId)}`)
}
export function getProductSaleUnits(productId) {
  return apiRequest(`/api/v1/catalog/products/${encodeURIComponent(productId)}`)
}
export function getCatalogCategories(options = {}) {
  return apiRequest('/api/v1/catalog/categories', options)
}
export function getCatalogBrands(options = {}) {
  return apiRequest('/api/v1/catalog/brands', options)
}
export function getPublicCategories({ parentId, roots = false } = {}, options = {}) {
  const query = new URLSearchParams()
  if (parentId != null) query.set('parentId', String(parentId))
  if (roots) query.set('roots', 'true')
  return apiRequest(`/api/v1/catalog/public-categories${query.size ? `?${query}` : ''}`, options)
}
// Monetary values are whole-VND decimal strings. Avoid Number conversion/rounding.
export function formatVnd(value) {
  if (!/^\d+$/.test(String(value))) throw new TypeError('Gi\u00e1 ti\u1ec1n ch\u01b0a h\u1ee3p l\u1ec7.')
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(BigInt(value))
}
