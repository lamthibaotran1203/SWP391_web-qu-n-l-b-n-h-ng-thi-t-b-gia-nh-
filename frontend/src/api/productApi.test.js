import { beforeEach, expect, it, vi } from 'vitest'
import { apiRequest } from './http'
import { getProducts } from './productApi'
vi.mock('./http', () => ({ apiRequest: vi.fn() }))
beforeEach(() => vi.resetAllMocks())
it('requests product-level results with ID filters and preserves whole VND strings and server page', async () => {
  apiRequest.mockResolvedValue({ items: [{ productId: 7, name: 'Nồi', minPrice: '999999999999999990', maxPrice: '999999999999999999', availableQuantity: 2, skuCount: 2 }], page: 1, size: 12, totalElements: 13 })
  const result = await getProducts({ search: ' nồi ', brandId: '3', categoryId: '5', availability: 'in_stock', page: 9 })
  const url = new URL(apiRequest.mock.calls[0][0], 'http://localhost')
  expect(url.pathname).toBe('/api/v1/catalog/products')
  expect(url.searchParams.get('search')).toBe('nồi')
  expect(url.searchParams.get('categoryId')).toBe('5')
  expect(url.searchParams.get('availability')).toBe('in_stock')
  expect(result.number).toBe(1)
  expect(result.totalPages).toBe(2)
  expect(result.content[0].price).toBe('999999999999999990')
  expect(result.content[0].maxPrice).toBe('999999999999999999')
})
