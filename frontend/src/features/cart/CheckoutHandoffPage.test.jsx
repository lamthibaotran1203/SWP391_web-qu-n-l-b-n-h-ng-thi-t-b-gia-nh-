import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import { cleanup, render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { AuthContext } from '../../auth/auth-context'
import { CheckoutHandoffPage } from './CheckoutHandoffPage'
import { getCart } from './api'

vi.mock('./api', () => ({ getCart: vi.fn() }))
afterEach(cleanup)
beforeEach(() => vi.resetAllMocks())

function show(user = { authenticated: true, role: 'CUSTOMER' }) {
  render(<AuthContext.Provider value={{ user }}><MemoryRouter initialEntries={['/checkout']}><Routes>
    <Route path="/checkout" element={<CheckoutHandoffPage />} />
    <Route path="/login" element={<p>Login required</p>} />
    <Route path="/products" element={<p>Public products</p>} />
  </Routes></MemoryRouter></AuthContext.Provider>)
}

it('rechecks the current cart and makes it clear no order has been created', async () => {
  getCart.mockResolvedValue({ items: [{ skuId: 1, name: 'Nồi', quantity: 2, lineTotal: '200000' }], subtotal: '200000', checkoutEligible: true })
  show()
  expect(await screen.findByRole('heading', { name: 'Thông tin giỏ hàng hiện tại' })).toBeInTheDocument()
  expect(screen.getByText('Bước đặt hàng UC14 đang chờ tích hợp.')).toBeInTheDocument()
  expect(getCart).toHaveBeenCalledTimes(1)
})

it('does not proceed when the cart has become empty or unavailable', async () => {
  getCart.mockResolvedValue({ items: [], subtotal: '0', checkoutEligible: false })
  show()
  expect(await screen.findByRole('alert')).toHaveTextContent('Giỏ hàng trống hoặc có sản phẩm cần cập nhật')
})

it('requires customer authentication before loading protected information', async () => {
  show({ authenticated: false })
  expect(await screen.findByText('Login required')).toBeInTheDocument()
  expect(getCart).not.toHaveBeenCalled()
})

it('requires login again when the session expires during retrieval', async () => {
  getCart.mockRejectedValue({ status: 401, message: 'Expired' })
  show()
  expect(await screen.findByText('Login required')).toBeInTheDocument()
})
