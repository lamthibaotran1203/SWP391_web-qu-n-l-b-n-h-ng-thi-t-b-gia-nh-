import { apiRequest } from './http'

export function getProducts({ search = '', brand = '', category = '', minPrice = '', maxPrice = '', sort = 'newest', page = 0, size = 12 } = {}) {
  const params = new URLSearchParams({ page, size })
  if (search.trim()) params.set('search', search.trim())
  if (brand.trim()) params.set('brand', brand.trim())
  if (category.trim()) params.set('category', category.trim())
  if (String(minPrice).trim()) params.set('minPrice', String(minPrice).trim())
  if (String(maxPrice).trim()) params.set('maxPrice', String(maxPrice).trim())
  if (sort) params.set('sort', sort)
  return apiRequest(`/api/v1/catalog/skus?${params.toString()}`).then((result) => ({
    content: result.items.map(toProduct),
    number: result.page,
    size: result.size,
    totalElements: result.totalElements,
    totalPages: Math.ceil(result.totalElements / result.size),
  }))
}

export function getProductById(id) {
  return apiRequest(`/api/v1/catalog/skus/${encodeURIComponent(id)}`).then(toProduct)
}

function toProduct(sku) {
  return {
    id: sku.skuId,
    productId: sku.productId,
    sku: sku.skuCode,
    // The card/detail title is the product name; skuName is only the sale-unit label.
    name: sku.productName || sku.skuName,
    brand: sku.brandName,
    category: sku.categoryName,
    price: sku.sellingPrice,
    stockQuantity: sku.availableQuantity,
    description: '',
    imageUrl: sku.imageUrl,
    status: sku.purchasable ? 'ACTIVE' : 'INACTIVE',
  }
}
