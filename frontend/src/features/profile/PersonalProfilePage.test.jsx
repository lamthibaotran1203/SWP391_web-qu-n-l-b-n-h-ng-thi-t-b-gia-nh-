import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { PersonalProfilePage } from './PersonalProfilePage'
import { useAuth } from '../../auth/useAuth'
import { getPersonalProfile } from './api'

vi.mock('../../auth/useAuth', () => ({ useAuth: vi.fn() }))
vi.mock('./api', () => ({ getPersonalProfile: vi.fn() }))
const profile = { email: 'customer@example.invalid', fullName: 'Nguyễn An', phone: '0901234567' }
const saveProfile = vi.fn()
const reload = vi.fn()

beforeEach(() => {
  vi.clearAllMocks()
  getPersonalProfile.mockResolvedValue(profile)
  saveProfile.mockResolvedValue(profile)
  useAuth.mockReturnValue({ user: { authenticated: true, role: 'CUSTOMER' }, connection: { status: 'connected' }, saveProfile, reload })
})
afterEach(cleanup)
function show() {
  render(<MemoryRouter initialEntries={['/account/profile']}><Routes>
    <Route path="/account/profile" element={<PersonalProfilePage />} />
    <Route path="/login" element={<p>Trang đăng nhập</p>} />
  </Routes></MemoryRouter>)
}

it('redirects guests without requesting profile data', () => {
  useAuth.mockReturnValue({ user: { authenticated: false }, connection: { status: 'connected' } })
  show()
  expect(screen.getByText('Trang đăng nhập')).toBeInTheDocument()
  expect(getPersonalProfile).not.toHaveBeenCalled()
})
it('keeps the Customer profile page inaccessible to other roles', () => {
  useAuth.mockReturnValue({ user: { authenticated: true, role: 'MANAGER' }, connection: { status: 'connected' } })
  show()
  expect(screen.getByRole('heading', { name: 'Trang hồ sơ dành cho khách hàng' })).toBeInTheDocument()
  expect(getPersonalProfile).not.toHaveBeenCalled()
})
it('loads safe fields and leaves email read-only', async () => {
  show()
  expect(await screen.findByLabelText('Họ và tên')).toHaveValue(profile.fullName)
  expect(screen.getByLabelText('Email')).toHaveAttribute('readonly')
  expect(screen.getByLabelText('Số điện thoại')).toHaveValue(profile.phone)
})
it('rejects invalid input before saving and cancel restores the loaded values', async () => {
  show()
  fireEvent.change(await screen.findByLabelText('Số điện thoại'), { target: { value: 'abc' } })
  fireEvent.change(screen.getByLabelText('Họ và tên'), { target: { value: 'Tên mới' } })
  fireEvent.click(screen.getByRole('button', { name: 'Lưu thay đổi' }))
  expect(screen.getByLabelText('Số điện thoại')).toHaveAttribute('aria-invalid', 'true')
  expect(saveProfile).not.toHaveBeenCalled()
  fireEvent.click(screen.getByRole('button', { name: 'Hủy thay đổi' }))
  expect(screen.getByLabelText('Họ và tên')).toHaveValue(profile.fullName)
  expect(screen.getByLabelText('Số điện thoại')).toHaveValue(profile.phone)
})
it('blocks duplicate submits and uses returned server values as the new saved baseline', async () => {
  let finish
  saveProfile.mockReturnValue(new Promise((resolve) => { finish = resolve }))
  show()
  fireEvent.change(await screen.findByLabelText('Họ và tên'), { target: { value: '  Nguyễn   Bảo  ' } })
  fireEvent.change(screen.getByLabelText('Số điện thoại'), { target: { value: '' } })
  fireEvent.click(screen.getByRole('button', { name: 'Lưu thay đổi' }))
  expect(screen.getByRole('button', { name: 'Đang lưu…' })).toBeDisabled()
  fireEvent.click(screen.getByRole('button', { name: 'Đang lưu…' }))
  expect(saveProfile).toHaveBeenCalledTimes(1)
  expect(saveProfile).toHaveBeenCalledWith({ fullName: 'Nguyễn Bảo', phone: '' })
  finish({ ...profile, fullName: 'Nguyễn Bảo', phone: null })
  expect(await screen.findByRole('status')).toHaveTextContent('Đã lưu thông tin tài khoản.')
  fireEvent.change(screen.getByLabelText('Họ và tên'), { target: { value: 'Chưa lưu' } })
  fireEvent.click(screen.getByRole('button', { name: 'Hủy thay đổi' }))
  expect(screen.getByLabelText('Họ và tên')).toHaveValue('Nguyễn Bảo')
})
it('retains the draft when storage fails and displays server field errors', async () => {
  saveProfile.mockRejectedValue({ status: 503, message: 'Chưa thể lưu.', fieldErrors: { fullName: 'Kiểm tra lại tên.' } })
  show()
  fireEvent.change(await screen.findByLabelText('Họ và tên'), { target: { value: 'Tên đang sửa' } })
  fireEvent.click(screen.getByRole('button', { name: 'Lưu thay đổi' }))
  expect(await screen.findByRole('alert')).toHaveTextContent('Chưa thể lưu.')
  expect(screen.getByLabelText('Họ và tên')).toHaveValue('Tên đang sửa')
  expect(screen.getByLabelText('Họ và tên')).toHaveAttribute('aria-invalid', 'true')
})
it('offers retry after a failed read', async () => {
  getPersonalProfile.mockRejectedValueOnce(new Error('Không tải được hồ sơ.')).mockResolvedValue(profile)
  show()
  fireEvent.click(await screen.findByRole('button', { name: 'Thử tải lại' }))
  expect(await screen.findByLabelText('Họ và tên')).toHaveValue(profile.fullName)
  await waitFor(() => expect(getPersonalProfile).toHaveBeenCalledTimes(2))
})
