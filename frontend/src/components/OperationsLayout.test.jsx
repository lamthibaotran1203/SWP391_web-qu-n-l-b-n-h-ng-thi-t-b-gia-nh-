import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { AuthContext } from '../auth/auth-context'
import { OperationsLayout, RequireRole } from './OperationsLayout'

function renderLayout({ role = 'ADMIN', path = '/admin/products', connection, logout } = {}) {
  const auth = {
    user: { authenticated: true, fullName: role === 'ADMIN' ? 'Bảo Trân' : 'Nhân viên kho', role },
    connection: connection || { status: 'connected', message: '' },
    reload: vi.fn(),
    logout: logout || vi.fn().mockResolvedValue(undefined),
  }

  return {
    auth,
    ...render(
      <MemoryRouter initialEntries={[path]}>
        <AuthContext.Provider value={auth}>
          <Routes>
            <Route path="/login" element={<p>Trang đăng nhập</p>} />
            <Route element={<OperationsLayout />}>
              <Route path="/staff/orders" element={<h1>Danh sách đơn hàng</h1>} />
              <Route element={<RequireRole roles={['ADMIN']} />}>
                <Route path="/admin/products" element={<h1>Danh sách sản phẩm</h1>} />
                <Route path="/admin/suppliers" element={<h1>Danh sách nhà cung cấp</h1>} />
              </Route>
            </Route>
          </Routes>
        </AuthContext.Provider>
      </MemoryRouter>,
    ),
  }
}

describe('OperationsLayout', () => {
  it('shows the complete Admin navigation and marks the current destination', () => {
    renderLayout()

    expect(screen.getByText('Quản trị viên')).toBeInTheDocument()
    expect(screen.queryByText('Bảo Trân')).not.toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Đơn hàng bán' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Sản phẩm & SKU' })).toHaveClass('active')
    expect(screen.getByRole('link', { name: 'Nhà cung cấp' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Lập đơn mua hàng' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Cài đặt tài khoản' })).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Danh sách sản phẩm' })).toBeInTheDocument()
  })

  it('limits Staff navigation and blocks a direct Admin URL', () => {
    renderLayout({ role: 'STAFF' })

    expect(screen.getByText('Nhân viên')).toBeInTheDocument()
    expect(screen.queryByText('Nhân viên kho')).not.toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Đơn hàng bán' })).toBeInTheDocument()
    expect(screen.queryByRole('link', { name: 'Sản phẩm & SKU' })).not.toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Bạn không có quyền truy cập' })).toBeInTheDocument()
  })

  it('opens and closes the responsive navigation', async () => {
    const user = userEvent.setup()
    const { container } = renderLayout()

    await user.click(screen.getByRole('button', { name: 'Mở menu' }))
    expect(container.querySelector('.operations-layout')).toHaveClass('operations-layout--open')
    await user.click(screen.getAllByRole('button', { name: 'Đóng menu' })[0])
    expect(container.querySelector('.operations-layout')).not.toHaveClass('operations-layout--open')
  })

  it('logs out once and returns to the login page', async () => {
    const user = userEvent.setup()
    const logout = vi.fn().mockResolvedValue(undefined)
    renderLayout({ logout })

    await user.click(screen.getByRole('button', { name: 'Đăng xuất' }))
    await waitFor(() => expect(logout).toHaveBeenCalledTimes(1))
    expect(await screen.findByText('Trang đăng nhập')).toBeInTheDocument()
  })
})
