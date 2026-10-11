import { act, cleanup, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import { getProducts } from '../api/productApi'
import { getCatalogBrands, getCatalogCategories } from '../features/catalog/api'
import { ProductListPage } from './ProductListPage'

vi.mock('../api/productApi', () => ({ getProducts: vi.fn(), getProductById: vi.fn() }))
vi.mock('../features/catalog/api', () => ({
  formatVnd: (value) => `${value} ₫`,
  getCatalogBrands: vi.fn(),
  getCatalogCategories: vi.fn(),
}))
const page = (name) => ({ content: [{ id: 42, productId: 100, name, sku: 'SKU', price: '200000', stockQuantity: 5 }], number: 0, totalPages: 1, totalElements: 1 })
beforeEach(() => {
  getProducts.mockReset()
  getCatalogBrands.mockReset()
  getCatalogCategories.mockReset()
  getProducts.mockResolvedValue(page('Nồi inox'))
  getCatalogBrands.mockResolvedValue([])
  getCatalogCategories.mockResolvedValue([])
})
afterEach(cleanup)

it('debounces suggestions without replacing results before search is submitted', async () => {
  getProducts
    .mockResolvedValueOnce(page('Nồi inox'))
    .mockResolvedValueOnce(page('Ấm điện gợi ý'))
    .mockResolvedValueOnce(page('Kết quả tìm kiếm'))
  const user = userEvent.setup()
  render(<MemoryRouter><ProductListPage /></MemoryRouter>)
  await screen.findByText('Nồi inox')
  expect(screen.getByRole('link', { name: /Nồi inox/ })).toHaveAttribute('href', '/products/100?sku=42')
  await user.type(screen.getByRole('textbox', { name: 'Tìm sản phẩm' }), 'ấm điện')
  expect(getProducts).toHaveBeenCalledTimes(1)
  expect(await screen.findByText('Ấm điện gợi ý', {}, { timeout: 1500 })).toBeInTheDocument()
  expect(screen.getByText('Nồi inox')).toBeInTheDocument()
  expect(getProducts.mock.calls[1][0]).toMatchObject({ search: 'ấm điện', page: 0, size: 5 })
  await user.click(screen.getByRole('button', { name: 'Tìm kiếm' }))
  expect(await screen.findByText('Kết quả tìm kiếm')).toBeInTheDocument()
  expect(getProducts.mock.calls[2][0].search).toBe('ấm điện')
  expect(screen.queryByLabelText('Gợi ý sản phẩm')).not.toBeInTheDocument()
})

it('applies filters, sorting, pagination and clear from the UI', async () => {
  getCatalogBrands.mockResolvedValue([{ id: 1, name: 'SUNHOUSE' }])
  getCatalogCategories.mockResolvedValue([{ id: 2, name: 'Nhà bếp' }])
  getProducts.mockResolvedValue({ ...page('Nồi inox'), totalPages: 2, totalElements: 13 })
  const user = userEvent.setup()
  render(<MemoryRouter><ProductListPage /></MemoryRouter>)
  await screen.findByText('Nồi inox')
  await user.selectOptions(screen.getByRole('combobox', { name: 'Lọc thương hiệu' }), 'SUNHOUSE')
  await user.selectOptions(screen.getByRole('combobox', { name: 'Lọc danh mục' }), 'Nhà bếp')
  await user.type(screen.getByRole('spinbutton', { name: 'Giá từ' }), '100000')
  await user.type(screen.getByRole('spinbutton', { name: 'Giá đến' }), '500000')
  await user.click(screen.getByRole('button', { name: 'Tìm kiếm' }))
  await waitFor(() => expect(getProducts).toHaveBeenCalledTimes(2))
  expect(getProducts.mock.calls[1][0]).toMatchObject({ brand: 'SUNHOUSE', category: 'Nhà bếp', minPrice: '100000', maxPrice: '500000', page: 0 })
  await user.selectOptions(screen.getByRole('combobox', { name: 'Sắp xếp' }), 'price_desc')
  await waitFor(() => expect(getProducts).toHaveBeenCalledTimes(3))
  expect(getProducts.mock.calls[2][0].sort).toBe('price_desc')
  await user.click(screen.getByRole('button', { name: 'Sau' }))
  await waitFor(() => expect(getProducts.mock.calls.at(-1)[0].page).toBe(1))
  await user.click(screen.getByRole('button', { name: 'Xóa' }))
  await waitFor(() => expect(getProducts.mock.calls.at(-1)[0]).toMatchObject({ search: '', brand: '', category: '', minPrice: '', maxPrice: '', sort: 'newest', page: 0 }))
})

it('shows no results with a short clear action', async () => {
  getProducts.mockResolvedValue({ content: [], number: 0, totalPages: 0, totalElements: 0 })
  const user = userEvent.setup()
  render(<MemoryRouter><ProductListPage /></MemoryRouter>)
  expect(await screen.findByRole('heading', { name: 'Không tìm thấy sản phẩm' })).toBeInTheDocument()
  await user.click(screen.getByRole('button', { name: 'Xóa' }))
  expect(screen.getByRole('textbox', { name: 'Tìm sản phẩm' })).toHaveValue('')
})

it('shows only retrieval error and retries without false no-results content', async () => {
  getProducts.mockRejectedValueOnce(new Error('Mất kết nối')).mockResolvedValueOnce(page('Đã tải lại'))
  const user = userEvent.setup()
  render(<MemoryRouter><ProductListPage /></MemoryRouter>)
  expect(await screen.findByRole('alert')).toHaveTextContent('Mất kết nối')
  expect(screen.queryByText('Không tìm thấy sản phẩm')).not.toBeInTheDocument()
  await user.click(screen.getByRole('button', { name: 'Thử lại' }))
  expect(await screen.findByText('Đã tải lại')).toBeInTheDocument()
})

it('reports reference loading failure and reloads brand/category filters', async () => {
  getCatalogBrands.mockRejectedValueOnce(new Error('Không tải được bộ lọc')).mockResolvedValueOnce([{ id: 1, name: 'SUNHOUSE' }])
  const user = userEvent.setup()
  render(<MemoryRouter><ProductListPage /></MemoryRouter>)
  expect(await screen.findByRole('alert')).toHaveTextContent('Không tải được bộ lọc')
  await user.click(screen.getByRole('button', { name: 'Tải lại bộ lọc' }))
  expect(await screen.findByRole('option', { name: 'SUNHOUSE' })).toBeInTheDocument()
  expect(screen.queryByRole('button', { name: 'Tải lại bộ lọc' })).not.toBeInTheDocument()
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
