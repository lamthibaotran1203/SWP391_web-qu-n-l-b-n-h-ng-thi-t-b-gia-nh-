import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import { getPosCatalog, processPosTransaction } from './api'
import { PosPage } from './PosPage'

vi.mock('./api', () => ({
  getPosCatalog: vi.fn(),
  processPosTransaction: vi.fn(),
}))

vi.mock('../catalog/api', () => ({
  formatVnd: (value) => `${value} ₫`,
}))

const catalog = {
  items: [
    {
      skuId: 71,
      productId: 8,
      productName: 'Ấm siêu tốc',
      skuCode: 'AM-001',
      skuName: '1.5 lít',
      sellingPrice: '350000',
      availableQuantity: 4,
      purchasable: true,
    },
    {
      skuId: 72,
      productId: 9,
      productName: 'Nồi cơm',
      skuCode: 'NC-001',
      skuName: '1 lít',
      sellingPrice: '500000',
      availableQuantity: 0,
      purchasable: false,
    },
  ],
  page: 0,
  size: 12,
  totalElements: 2,
}

describe('PosPage', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    vi.stubGlobal('crypto', { randomUUID: vi.fn(() => 'transaction-key') })
    getPosCatalog.mockResolvedValue(catalog)
    processPosTransaction.mockResolvedValue({
      id: 51,
      orderNumber: 'POS-a1b2c3',
      totalAmount: 350000,
      placedAt: '2026-10-19T00:00:00Z',
    })
  })

  it('loads real catalog items and creates a counter order using the selected items', async () => {
    const user = userEvent.setup()
    render(<MemoryRouter><PosPage /></MemoryRouter>)

    expect(await screen.findByText('Ấm siêu tốc')).toBeInTheDocument()
    const addButtons = screen.getAllByRole('button', { name: 'Thêm vào đơn' })
    expect(addButtons[0]).toBeEnabled()
    expect(addButtons[0].closest('article'))
      .toHaveTextContent('Tồn kho: 4')
    expect(screen.getByText('Nồi cơm').closest('article')
      .querySelector('button')).toBeDisabled()

    await user.click(addButtons[0])
    expect(screen.getAllByText('350000 ₫').length).toBeGreaterThan(0)
    await user.click(screen.getByRole('button', { name: 'Thanh toán và hoàn tất' }))

    await waitFor(() => {
      expect(processPosTransaction).toHaveBeenCalledWith(
        [{ skuId: 71, quantity: 1 }],
        'CASH',
        'transaction-key',
      )
    })
    expect(await screen.findByRole('heading', { name: 'Đã tạo đơn POS-a1b2c3' }))
      .toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Xem đơn hàng thật' }))
      .toHaveAttribute('href', '/staff/orders')
  })

  it('keeps the idempotency key when an ambiguous failure is retried', async () => {
    const user = userEvent.setup()
    processPosTransaction.mockRejectedValueOnce(new Error('Kết nối bị gián đoạn.'))
    render(<MemoryRouter><PosPage /></MemoryRouter>)

    await user.click((await screen.findAllByRole('button', { name: 'Thêm vào đơn' }))[0])
    await user.click(screen.getByRole('button', { name: 'Thanh toán và hoàn tất' }))
    expect(await screen.findByRole('alert')).toHaveTextContent('Kết nối bị gián đoạn.')
    await user.click(screen.getByRole('button', { name: 'Thanh toán và hoàn tất' }))

    await waitFor(() => expect(processPosTransaction).toHaveBeenCalledTimes(2))
    expect(processPosTransaction.mock.calls[0][2]).toBe('transaction-key')
    expect(processPosTransaction.mock.calls[1][2]).toBe('transaction-key')
  })
})
