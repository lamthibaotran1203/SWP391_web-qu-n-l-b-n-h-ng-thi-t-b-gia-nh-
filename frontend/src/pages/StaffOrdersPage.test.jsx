import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { AuthContext } from '../auth/auth-context'
import { getSalesOrderDetails, getSalesOrders } from '../api/http'
import { StaffOrdersPage } from './StaffOrdersPage'

vi.mock('../api/http', () => ({
  getSalesOrderDetails: vi.fn(),
  getSalesOrders: vi.fn(),
}))

const order = {
  id: 41,
  orderNumber: 'HG-2026-0041',
  channel: 'ONLINE',
  status: 'PROCESSING',
  paymentStatus: 'PAID',
  paymentMethod: 'CARD',
  customerName: 'Nguyễn Minh Anh',
  customerEmail: 'minhanh@example.invalid',
  customerPhone: '0901234567',
  totalAmount: 350000,
  placedAt: '2026-10-05T03:00:00Z',
}

const orderDetails = {
  ...order,
  shippingAddress: '12 Nguyễn Huệ, Quận 1',
  subtotal: 400000,
  discountAmount: 50000,
  shippingFee: 0,
  items: [{
    id: 1,
    productName: 'Ấm siêu tốc',
    productSku: 'AM-001',
    quantity: 1,
    unitPrice: 350000,
    lineTotal: 350000,
  }],
}

function renderPage(role = 'STAFF') {
  return render(
    <MemoryRouter initialEntries={['/staff/orders']}>
      <AuthContext.Provider value={{
        user: { authenticated: true, fullName: 'Nhân viên', role },
        connection: { status: 'connected', message: '' },
        reload: vi.fn(),
      }}>
        <Routes>
          <Route path="/staff/orders" element={<StaffOrdersPage />} />
          <Route path="/login" element={<p>Đăng nhập</p>} />
        </Routes>
      </AuthContext.Provider>
    </MemoryRouter>,
  )
}

describe('StaffOrdersPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    getSalesOrders.mockResolvedValue({
      content: [order],
      page: 0,
      size: 20,
      totalElements: 1,
      totalPages: 1,
    })
    getSalesOrderDetails.mockResolvedValue(orderDetails)
  })

  it('shows order search results and applies status filters', async () => {
    const user = userEvent.setup()
    renderPage()

    expect(await screen.findByRole('button', { name: 'Xem chi tiết đơn HG-2026-0041' }))
      .toBeInTheDocument()
    expect(screen.getByText('Nguyễn Minh Anh')).toBeInTheDocument()
    expect(screen.getAllByText('Nhân viên')).toHaveLength(2)

    await user.selectOptions(screen.getByLabelText('Kênh bán'), 'COUNTER')
    await user.selectOptions(screen.getByLabelText('Trạng thái đơn'), 'PROCESSING')
    await user.click(screen.getByRole('button', { name: 'Tìm đơn hàng' }))

    await waitFor(() => {
      expect(getSalesOrders).toHaveBeenLastCalledWith(expect.objectContaining({
        channel: 'COUNTER',
        status: 'PROCESSING',
        page: 0,
        size: 20,
      }))
    })
  })

  it('loads the order details when an order number is selected', async () => {
    const user = userEvent.setup()
    renderPage()

    await user.click(await screen.findByRole('button', {
      name: 'Xem chi tiết đơn HG-2026-0041',
    }))

    const dialog = await screen.findByRole('dialog', { name: 'HG-2026-0041' })
    expect(dialog).toBeInTheDocument()
    expect(within(dialog).getByText('Đang xử lý')).toBeInTheDocument()
    expect(within(dialog).getByText('Đã thanh toán')).toBeInTheDocument()
    expect(screen.getByText('Ấm siêu tốc')).toBeInTheDocument()
    expect(screen.getByText('12 Nguyễn Huệ, Quận 1')).toBeInTheDocument()
    expect(getSalesOrderDetails).toHaveBeenCalledWith(41)
  })

  it('blocks customer accounts from staff order search', async () => {
    renderPage('CUSTOMER')

    expect(await screen.findByText(/không có quyền tra cứu đơn hàng/i)).toBeInTheDocument()
    expect(getSalesOrders).not.toHaveBeenCalled()
  })
})
