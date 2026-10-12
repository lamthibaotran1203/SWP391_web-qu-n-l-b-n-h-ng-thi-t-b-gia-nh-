import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { ChangePasswordPage } from './ChangePasswordPage'
import { useAuth } from '../auth/useAuth'
import { changeOwnPassword } from '../api/http'

vi.mock('../auth/useAuth', () => ({ useAuth: vi.fn() }))
vi.mock('../api/http', () => ({ changeOwnPassword: vi.fn() }))
const reload = vi.fn()

beforeEach(() => {
  vi.clearAllMocks()
  useAuth.mockReturnValue({ user: { authenticated: true }, connection: { status: 'connected' }, reload })
})
afterEach(cleanup)

function showPage() {
  render(<MemoryRouter initialEntries={['/account/password']}><Routes>
    <Route path="/account/password" element={<ChangePasswordPage />} />
    <Route path="/login" element={<p>Trang đăng nhập</p>} />
  </Routes></MemoryRouter>)
}
function fill(confirmation = 'new password') {
  fireEvent.change(screen.getByLabelText('Mật khẩu hiện tại'), { target: { value: 'old password' } })
  fireEvent.change(screen.getByLabelText('Mật khẩu mới'), { target: { value: 'new password' } })
  fireEvent.change(screen.getByLabelText('Xác nhận mật khẩu mới'), { target: { value: confirmation } })
}
function submit() { fireEvent.click(screen.getByRole('button', { name: 'Đổi mật khẩu' })) }

it('redirects an anonymous visitor to login', () => {
  useAuth.mockReturnValue({ user: { authenticated: false }, connection: { status: 'connected' }, reload })
  showPage()
  expect(screen.getByText('Trang đăng nhập')).toBeInTheDocument()
})
it('shows a connection retry instead of treating an unavailable server as an anonymous session', () => {
  useAuth.mockReturnValue({ user: { authenticated: false }, connection: { status: 'error', message: 'Chưa kết nối được máy chủ.' }, reload })
  showPage()
  expect(screen.getByRole('alert')).toHaveTextContent('Chưa kết nối được máy chủ.')
  expect(screen.queryByText('Trang đăng nhập')).not.toBeInTheDocument()
  fireEvent.click(screen.getByRole('button', { name: 'Kết nối lại' }))
  expect(reload).toHaveBeenCalledOnce()
})
it('reveals and hides password fields only when requested without sending them', () => {
  showPage(); fill()
  const fields = ['Mật khẩu hiện tại', 'Mật khẩu mới', 'Xác nhận mật khẩu mới'].map((label) => screen.getByLabelText(label))
  fields.forEach((field) => expect(field).toHaveAttribute('type', 'password'))
  fireEvent.click(screen.getByLabelText('Hiện mật khẩu'))
  fields.forEach((field) => expect(field).toHaveAttribute('type', 'text'))
  fireEvent.click(screen.getByLabelText('Hiện mật khẩu'))
  fields.forEach((field) => expect(field).toHaveAttribute('type', 'password'))
  expect(changeOwnPassword).not.toHaveBeenCalled()
})
it('rejects mismatched confirmation before sending passwords', () => {
  showPage(); fill('different'); submit()
  expect(screen.getByText('Xác nhận mật khẩu mới chưa khớp.')).toBeInTheDocument()
  expect(changeOwnPassword).not.toHaveBeenCalled()
})
it('shows the authoritative server error without logging out', async () => {
  changeOwnPassword.mockRejectedValue({ status: 400, message: 'Sai mật khẩu hiện tại.', fieldErrors: { currentPassword: 'Kiểm tra lại.' } })
  showPage(); fill(); submit()
  expect(await screen.findByRole('alert')).toHaveTextContent('Sai mật khẩu hiện tại.')
  expect(screen.getByLabelText('Mật khẩu hiện tại')).toHaveAttribute('aria-invalid', 'true')
  expect(reload).not.toHaveBeenCalled()
})
it('blocks repeated submissions, clears password fields and refreshes the logged-out session', async () => {
  let resolve
  changeOwnPassword.mockReturnValue(new Promise((done) => { resolve = done }))
  showPage(); fill(); submit()
  const pending = screen.getByRole('button', { name: 'Đang đổi mật khẩu…' })
  expect(pending).toBeDisabled()
  fireEvent.click(pending)
  expect(changeOwnPassword).toHaveBeenCalledTimes(1)
  expect(changeOwnPassword).toHaveBeenCalledWith('old password', 'new password', 'new password')
  resolve({ message: 'Đã đổi mật khẩu.' })
  await waitFor(() => expect(reload).toHaveBeenCalledTimes(1))
  expect(screen.queryByLabelText('Mật khẩu hiện tại')).not.toBeInTheDocument()
  expect(screen.getByRole('link', { name: 'Đăng nhập lại' })).toHaveAttribute('href', '/login')
})
