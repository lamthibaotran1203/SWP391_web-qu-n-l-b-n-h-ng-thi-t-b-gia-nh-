import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import { cleanup, fireEvent, render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { ProductEditor } from './ProductEditor'
import { CatalogWorkspace } from './CatalogWorkspace'
import { ManagedProductListPage } from './ManagedProductListPage'
import { skuPayload } from './formValues'
import { useAuth } from '../../../auth/useAuth'
import { getManagedProducts } from './api'

vi.mock('../../../auth/useAuth', () => ({ useAuth: vi.fn() }))
vi.mock('./api', () => ({ getManagedProducts: vi.fn(), getManagedProduct: vi.fn(), createManagedProduct: vi.fn(),
  editManagedProduct: vi.fn(), addManagedSku: vi.fn(), editManagedSku: vi.fn() }))
vi.mock('../api', () => ({ getCatalogCategories: vi.fn(async () => []), getCatalogBrands: vi.fn(async () => []), formatVnd: (value) => value }))
afterEach(cleanup)
beforeEach(() => vi.clearAllMocks())

it('preserves whole-VND values beyond Number precision and rejects duplicate manual attributes', () => {
  const form = { sellingPrice: '999999999999999999', originalPrice: '999999999999999999', minStockThreshold: '2',
    attributeRows: [{ key: 'Dung tích', value: '2 lít' }] }
  expect(skuPayload(form).sellingPrice).toBe('999999999999999999')
  expect(() => skuPayload({ ...form, attributeRows: [...form.attributeRows, { key: ' Dung tích ', value: '3 lít' }] })).toThrow('trùng')
})

it('keeps edits visible and explains a server version conflict instead of overwriting it', async () => {
  const save = vi.fn().mockRejectedValue(new Error('Danh mục đã thay đổi. Tải lại trước khi lưu.'))
  render(<ProductEditor product={{ name: 'Nồi inox', categoryId: 1, brandId: 2, imageUrl: '/test.svg', status: 'ACTIVE' }}
    categories={[{ id: 1, name: 'Gia dụng' }]} brands={[{ id: 2, name: 'Nhãn mẫu' }]} onSave={save} onCancel={vi.fn()} />)
  fireEvent.change(screen.getByLabelText('Tên sản phẩm'), { target: { value: 'Nồi inox mới' } })
  fireEvent.click(screen.getByRole('button', { name: 'Lưu sản phẩm' }))
  expect(await screen.findByRole('alert')).toHaveTextContent('Tải lại trước khi lưu')
  expect(screen.getByLabelText('Tên sản phẩm')).toHaveValue('Nồi inox mới')
  expect(save).toHaveBeenCalledTimes(1)
})

it('prevents a non-manager from loading management data', () => {
  useAuth.mockReturnValue({ user: { authenticated: true, role: 'STAFF' }, connection: { status: 'connected' } })
  render(<MemoryRouter><CatalogWorkspace /></MemoryRouter>)
  expect(screen.getByRole('heading', { name: 'Không có quyền quản lý sản phẩm' })).toBeInTheDocument()
  expect(getManagedProducts).not.toHaveBeenCalled()
})

it('offers a retry when management data cannot load', async () => {
  useAuth.mockReturnValue({ user: { authenticated: true, role: 'MANAGER' }, connection: { status: 'connected' } })
  getManagedProducts.mockRejectedValue(new Error('Chưa thể tải danh mục.'))
  render(<MemoryRouter><Routes><Route element={<CatalogWorkspace />}><Route path="/" element={<ManagedProductListPage />} /></Route></Routes></MemoryRouter>)
  expect(await screen.findByRole('alert')).toHaveTextContent('Chưa thể tải danh mục.')
  expect(screen.getByRole('button', { name: 'Thử tải lại' })).toBeInTheDocument()
})

it('requires an explicit in-form confirmation before deactivating a published product', async () => {
  const save = vi.fn().mockResolvedValue(undefined)
  render(<ProductEditor product={{ name: 'Nồi inox', categoryId: 1, brandId: 2, imageUrl: '/test.svg', status: 'ACTIVE' }}
    categories={[{ id: 1, name: 'Gia dụng' }]} brands={[{ id: 2, name: 'Nhãn mẫu' }]} onSave={save} onCancel={vi.fn()} />)
  fireEvent.change(screen.getByLabelText('Trạng thái sản phẩm'), { target: { value: 'INACTIVE' } })
  fireEvent.click(screen.getByRole('button', { name: 'Lưu sản phẩm' }))
  expect(await screen.findByRole('alert')).toHaveTextContent('Xác nhận ngừng bán trước khi lưu.')
  expect(save).not.toHaveBeenCalled()
  fireEvent.click(screen.getByRole('checkbox'))
  fireEvent.click(screen.getByRole('button', { name: 'Lưu sản phẩm' }))
  expect(save).toHaveBeenCalledTimes(1)
})

it('preserves manual attribute names that also exist on the JavaScript object prototype', () => {
  const payload = skuPayload({ minStockThreshold: 0, attributeRows: [{ key: '__proto__', value: 'Thuộc tính nhập tay' }] })
  expect(JSON.parse(JSON.stringify(payload)).attributes.__proto__).toBe('Thuộc tính nhập tay')
})
