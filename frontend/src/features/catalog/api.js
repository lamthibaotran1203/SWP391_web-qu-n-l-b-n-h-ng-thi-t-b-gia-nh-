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
export function getCatalogCategories() {
  return apiRequest('/api/v1/catalog/categories')
}
export function getCatalogBrands() {
  return apiRequest('/api/v1/catalog/brands')
}
// Monetary values are whole-VND decimal strings. Avoid Number conversion/rounding.
export function formatVnd(value) {
  if (!/^\d+$/.test(String(value))) throw new TypeError('Giá tiền chưa hợp lệ.')
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(BigInt(value))
}
