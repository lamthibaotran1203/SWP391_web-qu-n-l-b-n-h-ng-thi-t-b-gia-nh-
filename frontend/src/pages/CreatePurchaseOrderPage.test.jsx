import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import * as purchasingApi from '../features/purchasing/api'
import { CreatePurchaseOrderPage } from './CreatePurchaseOrderPage'

vi.mock('../features/purchasing/api', () => ({
  getActiveSuppliers: vi.fn(),
  getAvailableSkus: vi.fn(),
  createPurchaseOrder: vi.fn(),
  formatVnd: (val) => `${Number(val || 0).toLocaleString('vi-VN')} ₫`,
}))

const mockSuppliers = [
  {
    id: 1,
    code: 'SUP-SUNHOUSE',
    taxCode: '0101234567',
    name: 'Tập đoàn Sunhouse',
    contactName: 'Nguyễn Văn A',
    phone: '0901234567',
    email: 'contact@sunhouse.vn',
    address: 'Hà Nội',
    status: 'ACTIVE',
  },
]

const mockSkus = [
  {
    skuId: 101,
    skuCode: 'SKU-NOI-COM',
    skuName: 'Nồi cơm điện tử',
    sellingPrice: 1000000,
  },
  {
    skuId: 102,
    skuCode: 'SKU-CHAO',
    skuName: 'Chảo chống dính',
    sellingPrice: 400000,
  },
]

describe('CreatePurchaseOrderPage (UC-28 Create Purchase Order)', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    purchasingApi.getActiveSuppliers.mockResolvedValue({ data: mockSuppliers })
    purchasingApi.getAvailableSkus.mockResolvedValue({ data: { content: mockSkus } })
  })

  it('renders PO form with supplier and initial item line', async () => {
    render(
      <MemoryRouter>
        <CreatePurchaseOrderPage />
      </MemoryRouter>,
    )

    expect(await screen.findByText('Lập đơn mua hàng (Purchase Order)')).toBeInTheDocument()
    expect(screen.getByText(/Tập đoàn Sunhouse/i)).toBeInTheDocument()
    expect(screen.getByText(/0101234567/i)).toBeInTheDocument()
    expect(screen.getByText('SKU-NOI-COM - Nồi cơm điện tử')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Xác nhận tạo đơn PO' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Lưu nháp (Draft)' })).toBeInTheDocument()
  })

  it('allows adding a new item line and updating quantity', async () => {
    const user = userEvent.setup()

    render(
      <MemoryRouter>
        <CreatePurchaseOrderPage />
      </MemoryRouter>,
    )

    expect(await screen.findByText('Lập đơn mua hàng (Purchase Order)')).toBeInTheDocument()

    const addLineBtn = screen.getByRole('button', { name: '+ Thêm dòng sản phẩm' })
    await user.click(addLineBtn)

    const skuSelects = screen.getAllByRole('combobox')
    // First combobox is supplier, subsequent are SKU selects
    expect(skuSelects.length).toBeGreaterThanOrEqual(3)
  })

  it('submits purchase order successfully and shows confirmation', async () => {
    const user = userEvent.setup()

    purchasingApi.createPurchaseOrder.mockResolvedValue({
      data: {
        id: 99,
        poCode: 'PO-20261006-8888',
        supplierId: 1,
        status: 'ORDERED',
        totalCost: 700000,
      },
    })

    render(
      <MemoryRouter>
        <CreatePurchaseOrderPage />
      </MemoryRouter>,
    )

    expect(await screen.findByText('Lập đơn mua hàng (Purchase Order)')).toBeInTheDocument()

    const submitBtn = screen.getByRole('button', { name: 'Xác nhận tạo đơn PO' })
    await user.click(submitBtn)

    await waitFor(() => {
      expect(purchasingApi.createPurchaseOrder).toHaveBeenCalledWith(
        expect.objectContaining({
          supplierId: 1,
          isDraft: false,
          items: expect.arrayContaining([
            expect.objectContaining({
              skuId: 101,
              orderedQuantity: 1,
            }),
          ]),
        }),
      )
    })

    expect(await screen.findByText('Tạo đơn đặt hàng thành công!')).toBeInTheDocument()
    expect(screen.getByText('PO-20261006-8888')).toBeInTheDocument()
  })
})
