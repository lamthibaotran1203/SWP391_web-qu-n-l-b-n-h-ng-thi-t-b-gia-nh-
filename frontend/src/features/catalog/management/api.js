import { apiRequest } from '../../../api/http'

const base = '/api/v1/manager/catalog/products'
export function getManagedProducts({ search = '', page = 0, status = '' } = {}) {
  const query = new URLSearchParams({ search, page: String(page), size: '20' })
  if (status) query.set('status', status)
  return apiRequest(`${base}?${query}`)
}
export function getManagedProduct(id) { return apiRequest(`${base}/${encodeURIComponent(id)}`) }
function write(path, method, body) {
  return apiRequest(path, { method, headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body) }, false)
}
export function createManagedProduct(product, skus) { return write(base, 'POST', { product, skus }) }
export function editManagedProduct(id, productVersion, product) { return write(`${base}/${id}`, 'PUT', { productVersion, product }) }
export function addManagedSku(id, productVersion, sku) { return write(`${base}/${id}/skus`, 'POST', { productVersion, sku }) }
export function editManagedSku(id, skuId, productVersion, skuVersion, sku) {
  return write(`${base}/${id}/skus/${skuId}`, 'PUT', { productVersion, skuVersion, sku })
}
