import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { App } from '../../../app/App'
import { useAuth } from '../../../auth/useAuth'
import { addManagedSku, createManagedProduct, editManagedProduct, editManagedSku, getManagedProduct, getManagedProducts } from './api'

vi.mock('../../../auth/useAuth', () => ({ useAuth: vi.fn() }))
vi.mock('../../../auth/AuthProvider', () => ({ AuthProvider: ({ children }) => children }))
vi.mock('./api', () => ({ getManagedProduct: vi.fn(), getManagedProducts: vi.fn(), createManagedProduct: vi.fn(),
  editManagedProduct: vi.fn(), addManagedSku: vi.fn(), editManagedSku: vi.fn() }))
vi.mock('../api', () => ({ getCatalogCategories: vi.fn(async () => [{ id: 1, name: 'Gia dụng' }]),
  getCatalogBrands: vi.fn(async () => [{ id: 2, name: 'SUNHOUSE' }]), formatVnd: (value) => `${value} ₫` }))

const product = { productId: 7, name: 'Nồi cơm SUNHOUSE 1.8 lít', description: 'Nồi cơm điện.', categoryId: 1, brandId: 2,
  imageUrl: '/test.svg', status: 'ACTIVE', productVersion: 4, skus: [{ skuId: 9, skuVersion: 2, skuCode: 'SHD8602',
    barcode: 'BAR-8602', skuName: 'Dung tích 1.8 lít', status: 'ACTIVE', originalPrice: '590000', sellingPrice: '590000',
    availableQuantity: 2, lowStock: true, minStockThreshold: 3, attributes: {}, imageUrl: null }] }

function open(path) {
  window.history.replaceState({}, '', path)
  render(<App />)
}

beforeEach(() => {
  vi.clearAllMocks()
  useAuth.mockReturnValue({ user: { authenticated: true, role: 'ADMIN' }, connection: { status: 'connected' } })
  getManagedProduct.mockResolvedValue(product)
  getManagedProducts.mockResolvedValue({ items: [product], totalElements: 1, page: 0, size: 20 })
})
afterEach(cleanup)

it('separates list, detail and edit through the actual application routes', async () => {
  open('/admin/catalog')
  expect(await screen.findByRole('link', { name: `Chi tiết ${product.name}` })).toBeInTheDocument()
  expect(window.location.pathname).toBe('/admin/products')
  expect(screen.queryByLabelText('Tên phân loại')).not.toBeInTheDocument()
  fireEvent.click(screen.getByRole('link', { name: `Chi tiết ${product.name}` }))
  expect(await screen.findByText('Sắp hết hàng')).toBeInTheDocument()
  expect(window.location.pathname).toBe('/admin/products/7')
  expect(screen.queryByLabelText('Tên sản phẩm')).not.toBeInTheDocument()
  fireEvent.click(screen.getByRole('link', { name: 'Chỉnh sửa sản phẩm' }))
  expect(await screen.findByLabelText('Tên sản phẩm')).toHaveValue(product.name)
  expect(window.location.pathname).toBe('/admin/products/7/edit')
  expect(screen.queryByRole('table')).not.toBeInTheDocument()
})

it('opens creation directly and submits the product plus initial SKU atomically', async () => {
  createManagedProduct.mockResolvedValue(product)
  open('/admin/products/new')
  fireEvent.change(await screen.findByLabelText('Tên sản phẩm'), { target: { value: 'Ấm siêu tốc 1.8 lít' } })
  fireEvent.change(screen.getByLabelText('Mã hàng (SKU)'), { target: { value: 'SHD1182' } })
  fireEvent.change(screen.getByLabelText('Mã vạch nội bộ'), { target: { value: 'BAR-1182' } })
  fireEvent.change(screen.getByLabelText('Tên phân loại'), { target: { value: 'Dung tích 1.8 lít' } })
  fireEvent.click(screen.getByRole('button', { name: 'Thêm sản phẩm' }))
  await waitFor(() => expect(createManagedProduct).toHaveBeenCalledTimes(1))
  expect(createManagedProduct.mock.calls[0][0].name).toBe('Ấm siêu tốc 1.8 lít')
  expect(createManagedProduct.mock.calls[0][1][0].skuCode).toBe('SHD1182')
  expect(await screen.findByText('Đã thêm sản phẩm.')).toBeInTheDocument()
  expect(window.location.pathname).toBe('/admin/products/7')
  expect(getManagedProducts).not.toHaveBeenCalled()
})

it('loads current versions for a direct edit and keeps entered values on a 409', async () => {
  editManagedProduct.mockRejectedValue(Object.assign(new Error('Thông tin đã thay đổi. Hãy tải lại.'), { status: 409 }))
  open('/admin/products/7/edit')
  fireEvent.change(await screen.findByLabelText('Tên sản phẩm'), { target: { value: 'Nồi cơm mới' } })
  fireEvent.click(screen.getByRole('button', { name: 'Lưu sản phẩm' }))
  expect(await screen.findByRole('alert')).toHaveTextContent('Hãy tải lại')
  expect(editManagedProduct).toHaveBeenCalledWith(7, 4, expect.objectContaining({ name: 'Nồi cơm mới' }))
  expect(screen.getByLabelText('Tên sản phẩm')).toHaveValue('Nồi cơm mới')
  expect(window.location.pathname).toBe('/admin/products/7/edit')
  getManagedProduct.mockResolvedValue({ ...product, productVersion: 5, name: 'Tên vừa được cập nhật' })
  fireEvent.click(screen.getByRole('button', { name: 'Làm mới dữ liệu' }))
  await waitFor(() => expect(screen.getByLabelText('Tên sản phẩm')).toHaveValue('Tên vừa được cập nhật'))
  editManagedProduct.mockResolvedValue(product)
  fireEvent.click(screen.getByRole('button', { name: 'Lưu sản phẩm' }))
  await waitFor(() => expect(editManagedProduct).toHaveBeenLastCalledWith(7, 5, expect.objectContaining({ name: 'Tên vừa được cập nhật' })))
})

it('edits an owned SKU using both versions and returns to product detail', async () => {
  editManagedSku.mockResolvedValue(product)
  open('/admin/products/7/skus/9/edit')
  fireEvent.change(await screen.findByLabelText('Giá bán (VND)'), { target: { value: '600000' } })
  fireEvent.click(screen.getByRole('button', { name: 'Lưu thay đổi' }))
  await waitFor(() => expect(editManagedSku).toHaveBeenCalledWith(7, 9, 4, 2, expect.objectContaining({ sellingPrice: '600000' })))
  expect(await screen.findByText('Đã lưu phân loại.')).toBeInTheDocument()
})

it('does not display an edit form for a SKU belonging to another product', async () => {
  open('/admin/products/7/skus/99/edit')
  expect(await screen.findByRole('alert')).toHaveTextContent('không thuộc sản phẩm')
  expect(screen.queryByRole('button', { name: 'Lưu thay đổi' })).not.toBeInTheDocument()
  expect(editManagedSku).not.toHaveBeenCalled()
})

it('does not send writes when canceling a new SKU', async () => {
  open('/admin/products/7/skus/new')
  expect(await screen.findByLabelText('Tên phân loại')).toHaveValue('')
  fireEvent.click(screen.getByRole('button', { name: 'Hủy' }))
  expect(await screen.findByText('Sắp hết hàng')).toBeInTheDocument()
  expect(addManagedSku).not.toHaveBeenCalled()
})

it('preserves search and status in the list URL without querying on each keystroke', async () => {
  open('/admin/products?search=nồi&status=ACTIVE&page=0')
  const field = await screen.findByLabelText('Tên sản phẩm')
  await screen.findByRole('table')
  expect(getManagedProducts).toHaveBeenLastCalledWith({ search: 'nồi', status: 'ACTIVE', page: 0 }, expect.any(Object))
  const requests = getManagedProducts.mock.calls.length
  fireEvent.change(field, { target: { value: 'Ấm siêu tốc' } })
  expect(getManagedProducts.mock.calls.length).toBe(requests)
  fireEvent.click(screen.getByRole('button', { name: 'Tìm kiếm' }))
  await waitFor(() => expect(new URLSearchParams(window.location.search).get('search')).toBe('Ấm siêu tốc'))
  expect(new URLSearchParams(window.location.search).get('status')).toBe('ACTIVE')
})
