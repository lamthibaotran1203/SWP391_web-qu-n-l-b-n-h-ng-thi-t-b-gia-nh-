import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { AuthContext } from '../auth/auth-context'
import { confirmSalesOrder, getSalesOrderDetails, getSalesOrders } from '../api/http'
import { StaffOrdersPage } from './StaffOrdersPage'

vi.mock('../api/http', () => ({
  confirmSalesOrder: vi.fn(),
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

const pendingOrder = {
  ...order,
  status: 'PENDING_PAYMENT',
  paymentStatus: 'PENDING',
}

const pendingOrderDetails = {
  ...orderDetails,
  status: 'PENDING_PAYMENT',
  paymentStatus: 'PENDING',
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
    vi.resetAllMocks()
    getSalesOrders.mockResolvedValue({
      content: [order],
      page: 0,
      size: 20,
      totalElements: 1,
      totalPages: 1,
    })
    getSalesOrderDetails.mockResolvedValue(orderDetails)
    confirmSalesOrder.mockResolvedValue({
      ...pendingOrderDetails,
      status: 'CONFIRMED',
    })
  })

  it('shows order search results and applies status filters', async () => {
    const user = userEvent.setup()
    renderPage()

    expect(await screen.findByRole('button', { name: 'Xem chi tiết đơn HG-2026-0041' }))
      .toBeInTheDocument()
    expect(screen.getByText('Nguyễn Minh Anh')).toBeInTheDocument()
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

  it('confirms a pending order and updates both dialog and list without reloading', async () => {
    const user = userEvent.setup()
    getSalesOrders.mockResolvedValueOnce({
      content: [pendingOrder],
      page: 0,
      size: 20,
      totalElements: 1,
      totalPages: 1,
    })
    getSalesOrderDetails.mockResolvedValueOnce(pendingOrderDetails)
    renderPage()

    await user.click(await screen.findByRole('button', {
      name: 'Xem chi tiết đơn HG-2026-0041',
    }))
    await user.click(await screen.findByRole('button', { name: 'Xác nhận đơn hàng' }))
    await user.click(screen.getByRole('button', { name: 'Xác nhận', exact: true }))

    const dialog = await screen.findByRole('dialog', { name: 'HG-2026-0041' })
    expect(within(dialog).getByText('Đã xác nhận')).toBeInTheDocument()
    expect(within(dialog).getByText('Chờ thanh toán')).toBeInTheDocument()
    expect(within(screen.getByRole('table')).getByText('Đã xác nhận')).toBeInTheDocument()
    expect(confirmSalesOrder).toHaveBeenCalledTimes(1)
    expect(getSalesOrders).toHaveBeenCalledTimes(1)
  })

  it('does not show confirmation for an order outside PENDING_PAYMENT', async () => {
    const user = userEvent.setup()
    renderPage()

    await user.click(await screen.findByRole('button', {
      name: 'Xem chi tiết đơn HG-2026-0041',
    }))

    expect(await screen.findByRole('dialog', { name: 'HG-2026-0041' })).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Xác nhận đơn hàng' })).not.toBeInTheDocument()
  })

  it('prevents duplicate confirmation requests while one is pending', async () => {
    const user = userEvent.setup()
    let resolveConfirmation
    confirmSalesOrder.mockReturnValue(new Promise((resolve) => {
      resolveConfirmation = resolve
    }))
    getSalesOrders.mockResolvedValueOnce({
      content: [pendingOrder],
      page: 0,
      size: 20,
      totalElements: 1,
      totalPages: 1,
    })
    getSalesOrderDetails.mockResolvedValueOnce(pendingOrderDetails)
    renderPage()

    await user.click(await screen.findByRole('button', {
      name: 'Xem chi tiết đơn HG-2026-0041',
    }))
    await user.click(await screen.findByRole('button', { name: 'Xác nhận đơn hàng' }))
    const confirmButton = screen.getByRole('button', { name: 'Xác nhận', exact: true })
    await user.dblClick(confirmButton)

    expect(confirmSalesOrder).toHaveBeenCalledTimes(1)
    expect(screen.getByRole('button', { name: 'Đang xác nhận…' })).toBeDisabled()

    resolveConfirmation({ ...pendingOrderDetails, status: 'CONFIRMED' })
    expect(await screen.findByText('Đã xác nhận')).toBeInTheDocument()
  })

  it('keeps the order unchanged after a failed request and allows retry', async () => {
    const user = userEvent.setup()
    confirmSalesOrder.mockRejectedValueOnce(new Error('Máy chủ đang bận.'))
      .mockResolvedValueOnce({ ...pendingOrderDetails, status: 'CONFIRMED' })
    getSalesOrders.mockResolvedValueOnce({
      content: [pendingOrder],
      page: 0,
      size: 20,
      totalElements: 1,
      totalPages: 1,
    })
    getSalesOrderDetails.mockResolvedValueOnce(pendingOrderDetails)
    renderPage()

    await user.click(await screen.findByRole('button', {
      name: 'Xem chi tiết đơn HG-2026-0041',
    }))
    await user.click(await screen.findByRole('button', { name: 'Xác nhận đơn hàng' }))
    await user.click(screen.getByRole('button', { name: 'Xác nhận', exact: true }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Máy chủ đang bận.')
    const dialog = await screen.findByRole('dialog', { name: 'HG-2026-0041' })
    expect(within(dialog).getAllByText('Chờ thanh toán')).toHaveLength(2)

    await user.click(screen.getByRole('button', { name: 'Xác nhận', exact: true }))

    expect(await within(dialog).findByText('Đã xác nhận')).toBeInTheDocument()
    expect(confirmSalesOrder).toHaveBeenCalledTimes(2)
  })

  it('shows confirmation errors and allows retry; conflicts can reload details', async () => {
    const user = userEvent.setup()
    const conflict = new Error('Trạng thái đơn đã thay đổi.')
    conflict.status = 409
    confirmSalesOrder.mockRejectedValueOnce(conflict)
      .mockResolvedValueOnce({ ...pendingOrderDetails, status: 'CONFIRMED' })
    getSalesOrders.mockResolvedValueOnce({
      content: [pendingOrder],
      page: 0,
      size: 20,
      totalElements: 1,
      totalPages: 1,
    })
    getSalesOrderDetails.mockResolvedValueOnce(pendingOrderDetails)
      .mockResolvedValueOnce({ ...pendingOrderDetails, status: 'PROCESSING' })
    renderPage()

    await user.click(await screen.findByRole('button', {
      name: 'Xem chi tiết đơn HG-2026-0041',
    }))
    await user.click(await screen.findByRole('button', { name: 'Xác nhận đơn hàng' }))
    await user.click(screen.getByRole('button', { name: 'Xác nhận', exact: true }))
    expect(await screen.findByRole('alert')).toHaveTextContent('Trạng thái đơn đã thay đổi.')
    expect(screen.getByRole('button', { name: 'Tải lại chi tiết đơn' })).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: 'Tải lại chi tiết đơn' }))
    const dialog = await screen.findByRole('dialog', { name: 'HG-2026-0041' })
    expect(within(dialog).getByText('Đang xử lý')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Xác nhận đơn hàng' })).not.toBeInTheDocument()
  })

  it('blocks customer accounts from staff order search', async () => {
    renderPage('CUSTOMER')

    expect(await screen.findByText(/không có quyền tra cứu đơn hàng/i)).toBeInTheDocument()
    expect(getSalesOrders).not.toHaveBeenCalled()
  })
})
