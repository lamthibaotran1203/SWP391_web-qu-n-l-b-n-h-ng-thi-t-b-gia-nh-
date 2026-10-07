import { act, cleanup, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import { getProducts } from '../api/productApi'
import { ProductListPage } from './ProductListPage'

vi.mock('../api/productApi', () => ({ getProducts: vi.fn(), getProductById: vi.fn() }))
vi.mock('../features/catalog/api', () => ({
  formatVnd: (value) => `${value} ₫`,
  getCatalogBrands: vi.fn().mockResolvedValue([]),
  getCatalogCategories: vi.fn().mockResolvedValue([]),
}))
const page = (name) => ({ content: [{ id: 42, productId: 100, name, sku: 'SKU', price: '200000', stockQuantity: 5 }], number: 0, totalPages: 1, totalElements: 1 })
beforeEach(() => { vi.clearAllMocks(); getProducts.mockResolvedValue(page('Nồi inox')) })
afterEach(cleanup)

it('only searches on submit, so typing never spams requests', async () => {
  const user = userEvent.setup()
  render(<MemoryRouter><ProductListPage /></MemoryRouter>)
  await screen.findByText('Nồi inox')
  expect(screen.getByRole('link', { name: /Nồi inox/ })).toHaveAttribute('href', '/products/100?sku=42')
  await user.type(screen.getByRole('textbox', { name: 'Tìm sản phẩm' }), 'ấm điện')
  expect(getProducts).toHaveBeenCalledTimes(1)
  await user.click(screen.getByRole('button', { name: 'Tìm kiếm' }))
  await waitFor(() => expect(getProducts).toHaveBeenCalledTimes(2))
  expect(getProducts.mock.calls[1][0].search).toBe('ấm điện')
})

it('ignores obsolete responses after a new search and aborts the old request', async () => {
  let resolveOld
  getProducts.mockReturnValueOnce(new Promise((resolve) => { resolveOld = resolve }))
  getProducts.mockResolvedValueOnce(page('Ấm mới'))
  const user = userEvent.setup()
  render(<MemoryRouter><ProductListPage /></MemoryRouter>)
  await user.type(screen.getByRole('textbox', { name: 'Tìm sản phẩm' }), 'ấm')
  await user.click(screen.getByRole('button', { name: 'Tìm kiếm' }))
  await screen.findByText('Ấm mới')
  expect(getProducts.mock.calls[0][1].signal.aborted).toBe(true)
  await act(async () => { resolveOld(page('Kết quả cũ')) })
  await waitFor(() => expect(screen.queryByText('Kết quả cũ')).not.toBeInTheDocument())
})
