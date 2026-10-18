import { afterEach, expect, it, vi } from 'vitest'
import { cleanup, fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { AppHeader } from './AppShell'
import { AuthContext } from '../auth/auth-context'

afterEach(cleanup)
function show(role = 'CUSTOMER', logout = vi.fn()) {
  render(<MemoryRouter><AuthContext.Provider value={{ user: { authenticated: true, fullName: 'Nguyễn An', role }, logout }}>
    <AppHeader /><button>Phía ngoài</button>
  </AuthContext.Provider></MemoryRouter>)
  return screen.getByLabelText('Tài khoản của bạn').closest('details')
}
it('places Customer profile and password links inside the avatar disclosure, outside the main navigation', () => {
  const menu = show()
  expect(menu).not.toHaveAttribute('open')
  expect(within(screen.getByRole('navigation', { name: 'Điều hướng chính' })).queryByText('Thông tin tài khoản')).not.toBeInTheDocument()
  fireEvent.click(screen.getByLabelText('Tài khoản của bạn'))
  expect(menu).toHaveAttribute('open')
  fireEvent.click(screen.getByRole('link', { name: 'Thông tin tài khoản' }))
  expect(menu).not.toHaveAttribute('open')
})
it('keeps password available to employees without exposing the Customer profile link', () => {
  const menu = show('ADMIN')
  fireEvent.click(screen.getByLabelText('Tài khoản của bạn'))
  expect(within(menu).queryByText('Thông tin tài khoản')).not.toBeInTheDocument()
  expect(screen.getByRole('link', { name: 'Đổi mật khẩu' })).toHaveAttribute('href', '/account/password')
})
it('keeps employee navigation public without exposing cart or management links', () => {
  show('ADMIN')
  const navigation = within(screen.getByRole('navigation', { name: 'Điều hướng chính' }))

  expect(navigation.getAllByRole('link')).toHaveLength(3)
  expect(navigation.getByRole('link', { name: 'Trang chủ' })).toHaveAttribute('href', '/')
  expect(navigation.getByRole('link', { name: 'Sản phẩm' })).toHaveAttribute('href', '/products')
  expect(navigation.getByRole('link', { name: 'Danh mục' })).toHaveAttribute('href', '/categories')
  expect(navigation.queryByText('Giỏ hàng')).not.toBeInTheDocument()
  expect(navigation.queryByText('Quản lý sản phẩm')).not.toBeInTheDocument()
  expect(navigation.queryByText('Đơn hàng')).not.toBeInTheDocument()
  expect(navigation.queryByText('Nhà cung cấp')).not.toBeInTheDocument()
})
it('shows the cart link for authenticated customers', () => {
  show('CUSTOMER')
  const navigation = within(screen.getByRole('navigation', { name: 'Điều hướng chính' }))
  expect(navigation.getByRole('link', { name: 'Giỏ hàng' })).toHaveAttribute('href', '/cart')
})
it('closes on Escape with focus restored and on an outside pointer press', () => {
  const menu = show()
  const trigger = screen.getByLabelText('Tài khoản của bạn')
  fireEvent.click(trigger)
  fireEvent.keyDown(document, { key: 'Escape' })
  expect(menu).not.toHaveAttribute('open')
  expect(trigger).toHaveFocus()
  fireEvent.click(trigger)
  fireEvent.pointerDown(screen.getByRole('button', { name: 'Phía ngoài' }))
  expect(menu).not.toHaveAttribute('open')
})
it('prevents duplicate logout and keeps a failed logout retryable', async () => {
  let fail
  const logout = vi.fn(() => new Promise((resolve, reject) => { fail = reject }))
  show('CUSTOMER', logout)
  fireEvent.click(screen.getByLabelText('Tài khoản của bạn'))
  fireEvent.click(screen.getByRole('button', { name: 'Đăng xuất' }))
  expect(screen.getByRole('button', { name: 'Đang đăng xuất…' })).toBeDisabled()
  fireEvent.click(screen.getByRole('button', { name: 'Đang đăng xuất…' }))
  expect(logout).toHaveBeenCalledTimes(1)
  fail(new Error('Chưa thể đăng xuất.'))
  expect(await screen.findByRole('alert')).toHaveTextContent('Chưa thể đăng xuất.')
  await waitFor(() => expect(screen.getByRole('button', { name: 'Đăng xuất' })).not.toBeDisabled())
})
