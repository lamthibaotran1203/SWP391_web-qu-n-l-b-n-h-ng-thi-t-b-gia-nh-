import { render, screen, cleanup, fireEvent, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, afterEach, it, expect, vi } from 'vitest'
import { AuthContext } from '../../auth/auth-context'
import { CartPage } from './CartPage'
import { getCart, updateCartItem, removeCartItems } from './api'
vi.mock('./api', () => ({ getCart: vi.fn(), updateCartItem: vi.fn(), removeCartItems: vi.fn() }))
const item = { skuId: 1, productId: 10, name: 'Nồi inox', skuName: '2 lít', skuCode: 'SKU-1', quantity: 2, unitPrice: '100000', lineTotal: '200000', availableQuantity: 5, eligible: true, available: true }
const cart = { items: [item], subtotal: '200000', checkoutEligible: true }
function show() { render(<AuthContext.Provider value={{ user: { authenticated: true, role: 'CUSTOMER' } }}><MemoryRouter><CartPage /></MemoryRouter></AuthContext.Provider>) }
beforeEach(() => { vi.resetAllMocks(); getCart.mockResolvedValue(cart) })
afterEach(cleanup)
it('saves only the final quantity when the customer changes it quickly', async () => {
  updateCartItem.mockResolvedValue({ ...cart, items: [{ ...item, quantity: 4, lineTotal: '400000' }], subtotal: '400000' })
  show()
  await screen.findByText('Nồi inox')
  fireEvent.change(screen.getByLabelText('Số lượng'), { target: { value: '3' } })
  fireEvent.change(screen.getByLabelText('Số lượng'), { target: { value: '4' } })
  expect(updateCartItem).not.toHaveBeenCalled()
  await waitFor(() => expect(updateCartItem).toHaveBeenCalledExactlyOnceWith(1, 4))
  await waitFor(() => expect(screen.getByLabelText('Số lượng')).toHaveValue(4))
})
it('loads current amounts, updates quantity and removes selected items', async () => {
  updateCartItem.mockResolvedValue({ ...cart, items: [{ ...item, quantity: 3, lineTotal: '300000' }], subtotal: '300000' })
  removeCartItems.mockResolvedValue({ items: [], subtotal: '0', checkoutEligible: false })
  const user = userEvent.setup(); show()
  await screen.findByRole('heading', { name: 'Nồi inox' })
  fireEvent.change(screen.getByLabelText('Số lượng'), { target: { value: '3' } })
  expect(screen.queryByRole('button', { name: 'Cập nhật' })).not.toBeInTheDocument()
  await waitFor(() => expect(updateCartItem).toHaveBeenCalledWith(1, 3))
  await user.click(screen.getByLabelText('Chọn tất cả'))
  await user.click(screen.getByRole('button', { name: 'Xóa đã chọn (1)' }))
  expect(await screen.findByText('Giỏ hàng đang trống')).toBeInTheDocument()
  expect(removeCartItems).toHaveBeenCalledWith([1])
})
it('rejects invalid quantities and preserves displayed cart when saving fails', async () => {
  updateCartItem.mockRejectedValue(new Error('Không thể lưu'))
  show(); await screen.findByText('Nồi inox')
  fireEvent.change(screen.getByLabelText('Số lượng'), { target: { value: '1.5' } })
  expect(updateCartItem).not.toHaveBeenCalled()
  fireEvent.change(screen.getByLabelText('Số lượng'), { target: { value: '3' } })
  expect(await screen.findByRole('alert')).toHaveTextContent('Không thể lưu')
  await waitFor(() => expect(screen.getByLabelText('Số lượng')).toHaveValue(2))
  expect(screen.getByText('Nồi inox')).toBeInTheDocument()
})
it('shows stale availability and keeps removal enabled for retired products', async () => {
  getCart.mockResolvedValue({ ...cart, items: [{ ...item, eligible: false, available: false }], checkoutEligible: false })
  show(); expect(await screen.findByText('Sản phẩm không còn được bán')).toBeInTheDocument()
  expect(screen.getByLabelText('Số lượng')).toBeDisabled()
  expect(screen.getByRole('button', { name: 'Xóa' })).toBeEnabled()
})
