import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import * as purchasingApi from '../features/purchasing/api'
import { SuppliersPage } from './SuppliersPage'

vi.mock('../features/purchasing/api', () => ({
  getSuppliers: vi.fn(),
  getActiveSuppliers: vi.fn(),
  getSupplier: vi.fn(),
  createSupplier: vi.fn(),
  updateSupplier: vi.fn(),
  deleteSupplier: vi.fn(),
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
    createdAt: '2026-10-01T00:00:00Z',
  },
  {
    id: 2,
    code: 'SUP-PHILIPS',
    taxCode: '0109998888',
    name: 'Philips Việt Nam',
    contactName: 'Trần Thị B',
    phone: '0909999888',
    email: 'info@philips.vn',
    address: 'TP. Hồ Chí Minh',
    status: 'INACTIVE',
    createdAt: '2026-10-02T00:00:00Z',
  },
]

describe('SuppliersPage (UC-27 Manage Suppliers)', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    purchasingApi.getSuppliers.mockResolvedValue({
      content: mockSuppliers,
      number: 0,
      totalPages: 1,
      totalElements: 2,
    })
  })

  it('renders supplier table and title correctly', async () => {
    render(
      <MemoryRouter>
        <SuppliersPage />
      </MemoryRouter>,
    )

    expect(screen.getByText('Quản lý nhà cung cấp')).toBeInTheDocument()

    expect(await screen.findByText('SUP-SUNHOUSE')).toBeInTheDocument()
    expect(screen.getByText('Tập đoàn Sunhouse')).toBeInTheDocument()
    expect(screen.getByText('Philips Việt Nam')).toBeInTheDocument()
    expect(screen.getByText('0101234567')).toBeInTheDocument()
    expect(screen.getByText('Hoạt động')).toBeInTheDocument()
    expect(screen.getByText('Tạm ngừng')).toBeInTheDocument()
  })

  it('opens registration modal when clicking "Thêm nhà cung cấp"', async () => {
    const user = userEvent.setup()

    render(
      <MemoryRouter>
        <SuppliersPage />
      </MemoryRouter>,
    )

    expect(await screen.findByText('SUP-SUNHOUSE')).toBeInTheDocument()

    const addButton = screen.getByRole('button', { name: /\+ Thêm nhà cung cấp/i })
    await user.click(addButton)

    expect(await screen.findByText('Đăng ký nhà cung cấp mới')).toBeInTheDocument()
    expect(screen.getByLabelText(/Mã nhà cung cấp/i)).toBeInTheDocument()
    expect(screen.getByLabelText(/Mã số thuế/i)).toBeInTheDocument()
    expect(screen.getByLabelText(/Tên nhà cung cấp/i)).toBeInTheDocument()
  })

  it('triggers search when submitting search form', async () => {
    const user = userEvent.setup()

    render(
      <MemoryRouter>
        <SuppliersPage />
      </MemoryRouter>,
    )

    expect(await screen.findByText('SUP-SUNHOUSE')).toBeInTheDocument()

    const searchInput = screen.getByPlaceholderText('Nhập mã, tên NCC, MST hoặc SĐT...')
    await user.type(searchInput, 'Sunhouse')

    const searchBtn = screen.getByRole('button', { name: 'Tìm kiếm' })
    await user.click(searchBtn)

    await waitFor(() => {
      expect(purchasingApi.getSuppliers).toHaveBeenCalledWith(
        expect.objectContaining({
          keyword: 'Sunhouse',
          page: 0,
        }),
      )
    })
  })
})
