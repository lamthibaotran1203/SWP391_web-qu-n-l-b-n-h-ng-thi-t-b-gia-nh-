import { apiRequest } from './http'

export function getProducts({ search = '', brandId = '', categoryId = '', minPrice = '', maxPrice = '', availability = 'any', sort = 'newest', page = 0, size = 12 } = {}, options = {}) {
  const params = new URLSearchParams({ page, size })
  if (search.trim()) params.set('search', search.trim())
  if (String(brandId).trim()) params.set('brandId', String(brandId).trim())
  if (String(categoryId).trim()) params.set('categoryId', String(categoryId).trim())
  params.set('availability', availability)
  if (String(minPrice).trim()) params.set('minPrice', String(minPrice).trim())
  if (String(maxPrice).trim()) params.set('maxPrice', String(maxPrice).trim())
  if (sort) params.set('sort', sort)
  return apiRequest(`/api/v1/catalog/products?${params.toString()}`, options).then((result) => ({
    content: result.items.map((product) => ({ ...product, id: product.productId,
      brand: product.brandName, category: product.categoryName, price: product.minPrice,
      maxPrice: product.maxPrice, stockQuantity: product.availableQuantity })),
    number: result.page,
    size: result.size,
    totalElements: result.totalElements,
    totalPages: Math.ceil(result.totalElements / result.size),
  }))
}

export function getProductById(id, options = {}) {
  return apiRequest(`/api/v1/catalog/skus/${encodeURIComponent(id)}`, options).then(toProduct)
}

export function getProductDetail(productId, options = {}) {
  return apiRequest(`/api/v1/catalog/products/${encodeURIComponent(productId)}/detail`, options)
}

function toProduct(sku) {
  return {
    id: sku.skuId,
    productId: sku.productId,
    productVersion: sku.productVersion,
    skuVersion: sku.skuVersion,
    sku: sku.skuCode,
    // The card/detail title is the product name; skuName is only the sale-unit label.
    name: sku.productName || sku.skuName,
    brand: sku.brandName,
    category: sku.categoryName,
    price: sku.sellingPrice,
    stockQuantity: sku.availableQuantity,
    description: '',
    imageUrl: sku.imageUrl,
    status: 'ACTIVE',
  }
}
