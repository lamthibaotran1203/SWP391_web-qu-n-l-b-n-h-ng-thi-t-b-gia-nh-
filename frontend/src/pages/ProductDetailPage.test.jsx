import { act, cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { getProductDetail } from '../api/productApi'
import { AuthContext } from '../auth/auth-context'
import { ProductDetailPage } from './ProductDetailPage'

vi.mock('../api/productApi', () => ({ getProductDetail: vi.fn() }))
const sku = (skuId, stock = 5) => ({ skuId, skuCode: `SKU-${skuId}`, skuName: `${skuId} lít`, sellingPrice: `${skuId}00000`, availableQuantity: stock, purchasable: stock > 0, imageUrl: `/sku-${skuId}.svg` })
const detail = (skus = [sku(2)]) => ({ productId: 100, name: 'Nồi inox', description: 'Đáy dày\nDùng bếp từ', brandName: 'HomeMart', categoryName: 'Nhà bếp', images: ['/cover.svg', ...skus.map((s) => s.imageUrl)], skus })
function show({ onAddToCart, user = { authenticated: true, role: 'CUSTOMER' }, path = '/products/100' } = {}) {
  return render(<AuthContext.Provider value={{ user }}><MemoryRouter initialEntries={[path]}><Routes>
    <Route path="/products/:id" element={<ProductDetailPage onAddToCart={onAddToCart} />} />
    <Route path="/login" element={<p>Đăng nhập lại để mua hàng</p>} />
  </Routes></MemoryRouter></AuthContext.Provider>)
}
beforeEach(() => { vi.resetAllMocks(); getProductDetail.mockResolvedValue(detail()) })
afterEach(cleanup)

it('requires authentication again when the cart session expires', async () => {
  show({ onAddToCart: vi.fn().mockRejectedValue({ status: 401 }) })
  await screen.findByRole('heading', { name: 'Nồi inox' })
  await userEvent.click(screen.getByRole('button', { name: 'Thêm vào giỏ hàng' }))
  expect(await screen.findByText('Đăng nhập lại để mua hàng')).toBeInTheDocument()
})

it('loads public product metadata and automatically selects its only SKU', async () => {
  show()
  await screen.findByRole('heading', { name: 'Nồi inox' })
  expect(getProductDetail).toHaveBeenCalledWith('100', expect.objectContaining({ signal: expect.any(AbortSignal) }))
  expect(screen.getByText(/Đáy dày/)).toBeInTheDocument()
  expect(screen.getAllByText('HomeMart').length).toBeGreaterThan(0)
  expect(screen.getByLabelText('Phiên bản / quy cách')).toHaveValue('2')
  expect(screen.getByText('Còn 5 sản phẩm')).toBeInTheDocument()
  expect(screen.getByRole('button', { name: 'Thêm vào giỏ hàng' })).toBeDisabled()
  expect(screen.getByText(/Giỏ hàng hiện chưa sẵn sàng/)).toBeInTheDocument()
})

it('requires a choice for multiple SKUs and resets quantity/image when changing variant', async () => {
  getProductDetail.mockResolvedValue(detail([sku(2), sku(3, 0)]))
  show({ onAddToCart: vi.fn() })
  const select = await screen.findByLabelText('Phiên bản / quy cách')
  expect(select).toHaveValue('')
  fireEvent.change(select, { target: { value: '2' } })
  fireEvent.change(screen.getByLabelText('Số lượng'), { target: { value: '4' } })
  fireEvent.change(select, { target: { value: '3' } })
  expect(screen.getByLabelText('Số lượng')).toHaveValue(1)
  expect(screen.getByText('Hết hàng')).toBeInTheDocument()
  expect(screen.getByRole('img')).toHaveAttribute('src', '/sku-3.svg')
  expect(screen.getByRole('button', { name: 'Thêm vào giỏ hàng' })).toBeDisabled()
})

it('preselects the eligible SKU chosen in UC-08, but ignores unknown SKU IDs', async () => {
  getProductDetail.mockResolvedValue(detail([sku(2), sku(3)]))
  const view = show({ path: '/products/100?sku=3' })
  expect(await screen.findByLabelText('Phiên bản / quy cách')).toHaveValue('3')
  view.unmount()
  show({ path: '/products/100?sku=999' })
  expect(await screen.findByLabelText('Phiên bản / quy cách')).toHaveValue('')
})

it('hands only SKU ID and valid quantity to the future UC-10 adapter', async () => {
  const onAddToCart = vi.fn().mockResolvedValue(undefined)
  show({ onAddToCart })
  const quantity = await screen.findByLabelText('Số lượng')
  for (const invalid of ['0', '6', '1.5', '']) {
    fireEvent.change(quantity, { target: { value: invalid } })
    expect(screen.getByRole('button', { name: 'Thêm vào giỏ hàng' })).toBeDisabled()
  }
  fireEvent.change(quantity, { target: { value: '2' } })
  await userEvent.click(screen.getByRole('button', { name: 'Thêm vào giỏ hàng' }))
  await waitFor(() => expect(onAddToCart).toHaveBeenCalledExactlyOnceWith({ skuId: 2, quantity: 2 }))
})

it.each([{ authenticated: false }, { authenticated: true, role: 'ADMIN' }])('does not allow a non-Customer to hand off a cart operation: %j', async (user) => {
  show({ user, onAddToCart: vi.fn() })
  expect(await screen.findByRole('button', { name: 'Thêm vào giỏ hàng' })).toBeDisabled()
})

it('switches gallery images and falls back when an image fails', async () => {
  show()
  await screen.findByRole('heading', { name: 'Nồi inox' })
  await userEvent.click(screen.getByRole('button', { name: 'Xem ảnh 1' }))
  expect(screen.getByRole('img')).toHaveAttribute('src', '/cover.svg')
  fireEvent.error(screen.getByRole('img'))
  expect(screen.getByText('Chưa có hình ảnh')).toBeInTheDocument()
})

it('prevents duplicate handoffs and variant changes while a handoff is pending', async () => {
  let finish
  const onAddToCart = vi.fn().mockReturnValue(new Promise((resolve) => { finish = resolve }))
  show({ onAddToCart })
  await screen.findByRole('heading', { name: 'Nồi inox' })
  await userEvent.click(screen.getByRole('button', { name: 'Thêm vào giỏ hàng' }))
  expect(screen.getByRole('button', { name: 'Đang chuyển đến giỏ hàng...' })).toBeDisabled()
  expect(screen.getByLabelText('Phiên bản / quy cách')).toBeDisabled()
  expect(onAddToCart).toHaveBeenCalledTimes(1)
  await act(async () => { finish() })
})

it('cancels retrieval when leaving the detail page', async () => {
  const view = show()
  await screen.findByRole('heading', { name: 'Nồi inox' })
  const signal = getProductDetail.mock.calls[0][1].signal
  view.unmount()
  expect(signal.aborted).toBe(true)
})

it('does not render hidden product information on 404', async () => {
  getProductDetail.mockRejectedValue({ status: 404 })
  show()
  expect(await screen.findByRole('alert')).toHaveTextContent('không được công khai')
  expect(screen.queryByRole('heading', { name: 'Nồi inox' })).not.toBeInTheDocument()
})

it('retries retrieval failure and displays handoff errors without false success', async () => {
  getProductDetail.mockRejectedValueOnce(new Error('Mất kết nối'))
  const onAddToCart = vi.fn().mockRejectedValue(new Error('Tồn kho đã thay đổi'))
  show({ onAddToCart })
  await screen.findByText('Mất kết nối')
  await userEvent.click(screen.getByRole('button', { name: 'Thử lại' }))
  await screen.findByRole('heading', { name: 'Nồi inox' })
  await userEvent.click(screen.getByRole('button', { name: 'Thêm vào giỏ hàng' }))
  expect(await screen.findByRole('alert')).toHaveTextContent('Tồn kho đã thay đổi')
})
